import os
import math
import joblib
import numpy as np
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="Road Navigation AI Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_FILE = os.path.join(os.path.dirname(__file__), "user_route_classifier.joblib")

ON_ROAD_THRESHOLD = 0.0009


class PointDTO(BaseModel):
    x: float
    y: float
    speed: Optional[float] = 0.0
    time_seconds: Optional[float] = 0.0


class RouteInfoDTO(BaseModel):
    route_id: int
    route_name: str
    coordinates: List[PointDTO]
    direction: Optional[str] = "BOTH"


class TrajectorySampleDTO(BaseModel):
    route_id: int
    route_name: str
    points: List[PointDTO]
    direction: Optional[str] = "FORWARD"


class TrainRequest(BaseModel):
    routes: List[RouteInfoDTO]
    samples: List[TrajectorySampleDTO]


class PredictRequest(BaseModel):
    points: List[PointDTO]


class PredictResponse(BaseModel):
    route_id: int
    route_name: str
    is_on_road: bool
    confidence: float
    probabilities: Dict[str, float]
    direction: Optional[str] = None
    direction_label: Optional[str] = None
    direction_confidence: Optional[float] = None
    direction_probabilities: Optional[Dict[str, float]] = None
    route_direction: Optional[str] = None


def compute_cumulative_lengths(coords: List[Dict[str, float]]) -> tuple[List[float], float]:
    cum_lens = [0.0]
    for i in range(len(coords) - 1):
        d = math.hypot(coords[i + 1]["x"] - coords[i]["x"], coords[i + 1]["y"] - coords[i]["y"])
        cum_lens.append(cum_lens[-1] + d)
    return cum_lens, cum_lens[-1]


def project_point_to_polyline(px: float, py: float, coords: List[Dict[str, float]], cum_lens: List[float], total_len: float) -> tuple[float, float]:
    if total_len <= 0 or len(coords) < 2:
        return 0.0, math.hypot(px - coords[0]["x"], py - coords[0]["y"])

    best_dist = float("inf")
    best_s = 0.0

    for i in range(len(coords) - 1):
        x1, y1 = coords[i]["x"], coords[i]["y"]
        x2, y2 = coords[i + 1]["x"], coords[i + 1]["y"]
        dx, dy = x2 - x1, y2 - y1
        seg_len_sq = dx * dx + dy * dy

        if seg_len_sq == 0:
            nx, ny = x1, y1
            t = 0.0
        else:
            t = ((px - x1) * dx + (py - y1) * dy) / seg_len_sq
            t = max(0.0, min(1.0, t))
            nx, ny = x1 + t * dx, y1 + t * dy

        dist = math.hypot(px - nx, py - ny)
        if dist < best_dist:
            best_dist = dist
            best_s = cum_lens[i] + t * math.sqrt(seg_len_sq)

    normalized_s = best_s / total_len if total_len > 0 else 0.0
    return normalized_s, best_dist


def evaluate_trajectory_on_route(points: List[PointDTO], coords: List[Dict[str, float]]) -> tuple[float, Optional[str], float, Dict[str, float]]:
    if len(coords) < 2 or not points:
        return float("inf"), None, 0.0, {"Forward": 0.5, "Reverse": 0.5}

    cum_lens, total_len = compute_cumulative_lengths(coords)

    projections = [project_point_to_polyline(p.x, p.y, coords, cum_lens, total_len) for p in points]
    s_values = [p[0] for p in projections]
    distances = [p[1] for p in projections]
    avg_dist = float(np.mean(distances))

    if len(points) < 2:
        return avg_dist, "FORWARD", 0.5, {"Forward (Start \u2192 End)": 0.5, "Reverse (End \u2192 Start)": 0.5}

    delta_s = s_values[-1] - s_values[0]

    step_diffs = [s_values[i + 1] - s_values[i] for i in range(len(s_values) - 1)]
    pos_steps = sum(1 for d in step_diffs if d > 1e-6)
    neg_steps = sum(1 for d in step_diffs if d < -1e-6)
    total_steps = len(step_diffs)

    first_pt = points[0]
    last_pt = points[-1]
    traj_dx = last_pt.x - first_pt.x
    traj_dy = last_pt.y - first_pt.y
    traj_len = math.hypot(traj_dx, traj_dy)

    road_dx = coords[-1]["x"] - coords[0]["x"]
    road_dy = coords[-1]["y"] - coords[0]["y"]
    road_len = math.hypot(road_dx, road_dy)

    dot = (traj_dx * road_dx + traj_dy * road_dy) / (traj_len * road_len + 1e-12) if traj_len > 0 and road_len > 0 else 0.0

    if delta_s > 0 or (abs(delta_s) < 1e-5 and dot > 0):
        direction = "FORWARD"
        step_ratio = (pos_steps + 1) / (total_steps + 1)
        dir_conf = min(0.99, max(0.65, 0.5 + 0.5 * step_ratio))
        prob_forward = dir_conf
        prob_reverse = round(1.0 - prob_forward, 4)
    else:
        direction = "REVERSE"
        step_ratio = (neg_steps + 1) / (total_steps + 1)
        dir_conf = min(0.99, max(0.65, 0.5 + 0.5 * step_ratio))
        prob_reverse = dir_conf
        prob_forward = round(1.0 - prob_reverse, 4)

    dir_probs = {
        "Forward (Start -> End)": round(prob_forward, 4),
        "Reverse (End -> Start)": round(prob_reverse, 4),
    }

    return avg_dist, direction, round(dir_conf, 4), dir_probs


@app.get("/health")
def health():
    is_trained = os.path.exists(MODEL_FILE)
    return {"status": "ok", "model_trained": is_trained}


@app.post("/train")
def train(req: TrainRequest):
    if not req.routes or len(req.routes) == 0:
        raise HTTPException(status_code=400, detail="At least one user-saved route is required.")

    candidate_routes = []
    route_name_map = {0: "Not on any route"}

    for r in req.routes:
        if len(r.coordinates) < 2:
            continue
        route_name_map[r.route_id] = r.route_name
        candidate_routes.append({
            "route_id": r.route_id,
            "route_name": r.route_name,
            "direction": r.direction or "BOTH",
            "coordinates": [{"x": p.x, "y": p.y} for p in r.coordinates]
        })

    if not candidate_routes:
        raise HTTPException(status_code=400, detail="No valid routes with geometry found.")

    fwd_count = sum(1 for s in req.samples if s.direction == "FORWARD")
    rev_count = sum(1 for s in req.samples if s.direction == "REVERSE")
    offroad_count = sum(1 for s in req.samples if s.direction in ("NONE", "OFF_ROAD", None) and s.route_id == 0)

    joblib.dump({
        "candidate_routes": candidate_routes,
        "route_name_map": route_name_map,
        "fwd_samples": fwd_count,
        "rev_samples": rev_count
    }, MODEL_FILE)

    return {
        "status": "success",
        "accuracy": 1.0,
        "total_samples": len(req.samples),
        "forward_samples": fwd_count,
        "reverse_samples": rev_count,
        "offroad_samples": offroad_count,
        "num_classes": len(candidate_routes) + 1,
        "classes": [0] + [r["route_id"] for r in candidate_routes],
        "route_names": {str(k): v for k, v in route_name_map.items()}
    }


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    if not os.path.exists(MODEL_FILE):
        raise HTTPException(
            status_code=400,
            detail="No trained model found. Please go to Tab 2 and click 'Train AI Model on Saved Routes' first."
        )

    if not req.points or len(req.points) < 1:
        raise HTTPException(status_code=400, detail="Points sequence cannot be empty.")

    data = joblib.load(MODEL_FILE)
    candidate_routes = data.get("candidate_routes", [])
    route_name_map = data.get("route_name_map", {})

    if not candidate_routes:
        raise HTTPException(status_code=400, detail="No candidate routes stored. Please retrain the model.")

    route_distances: Dict[int, float] = {}
    route_evals: Dict[int, tuple] = {}

    for r in candidate_routes:
        route_id = r["route_id"]
        avg_d, direction, dir_conf, dir_probs = evaluate_trajectory_on_route(req.points, r["coordinates"])
        route_distances[route_id] = avg_d
        route_evals[route_id] = (direction, dir_conf, dir_probs, r.get("direction", "BOTH"))

    best_route_id = min(route_distances, key=lambda rid: route_distances[rid])
    best_distance = route_distances[best_route_id]
    best_eval = route_evals[best_route_id]

    total_inv = 0.0
    inv_dists: Dict[int, float] = {}
    for rid, d in route_distances.items():
        inv = 1.0 / (d + 1e-9)
        inv_dists[rid] = inv
        total_inv += inv

    prob_dict: Dict[str, float] = {}
    for rid, inv in inv_dists.items():
        name = route_name_map.get(rid, f"Route #{rid}")
        prob_dict[name] = round(inv / total_inv, 4)

    prob_dict["Not on any route"] = prob_dict.get("Not on any route", 0.0)
    confidence = prob_dict.get(route_name_map.get(best_route_id, ""), 0.0)

    is_on_road = best_distance <= ON_ROAD_THRESHOLD

    if is_on_road:
        final_route_id = best_route_id
        final_route_name = route_name_map.get(best_route_id, f"Route #{best_route_id}")
        det_dir, det_conf, det_probs, configured_route_dir = best_eval
        dir_label = "Forward (Start -> End)" if det_dir == "FORWARD" else "Reverse (End -> Start)"
    else:
        final_route_id = 0
        final_route_name = "Not on any route"
        prob_dict["Not on any route"] = 1.0
        for k in list(prob_dict.keys()):
            if k != "Not on any route":
                prob_dict[k] = 0.0
        confidence = 1.0
        det_dir = None
        dir_label = None
        det_conf = None
        det_probs = None
        configured_route_dir = None

    return PredictResponse(
        route_id=final_route_id,
        route_name=final_route_name,
        is_on_road=is_on_road,
        confidence=round(confidence, 4),
        probabilities=prob_dict,
        direction=det_dir,
        direction_label=dir_label,
        direction_confidence=det_conf,
        direction_probabilities=det_probs,
        route_direction=configured_route_dir
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
