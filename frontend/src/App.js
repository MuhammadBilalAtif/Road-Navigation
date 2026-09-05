import React, { useEffect, useRef, useState } from 'react';
import 'ol/ol.css';
import { Feature, Map, View } from 'ol';
import { Tile as TileLayer, Vector as VectorLayer } from 'ol/layer';
import { OSM, Vector as VectorSource } from 'ol/source';
import { LineString, Point } from 'ol/geom';
import { Circle, Fill, Stroke, Style } from 'ol/style';

const API_BASE = 'http://localhost:8081/api';

function App() {
    const mapRef = useRef();
    const pathSource = useRef(new VectorSource());
    const clickSource = useRef(new VectorSource());
    const sampleSource = useRef(new VectorSource());
    const medianSource = useRef(new VectorSource());
    const animVehicleSource = useRef(new VectorSource());
    const aiTestSeqSource = useRef(new VectorSource());
    const aiMatchedRoadSource = useRef(new VectorSource());
    const currentPathCoords = useRef([]);

    const [activeTab, setActiveTab] = useState('create_route');
    const [clicks, setClicks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [statusText, setStatusText] = useState('Islamabad Navigation Ready');
    const [pathLength, setPathLength] = useState(null);
    const [pathReady, setPathReady] = useState(false);
    const [routeName, setRouteName] = useState('');
    const [routeDirection, setRouteDirection] = useState('BOTH');
    const [genDirection, setGenDirection] = useState('ROUTE_DEFAULT');
    const [isSimulating, setIsSimulating] = useState(false);
    const animFrameId = useRef(null);
    const animStartTime = useRef(null);
    const [noiseLevel, setNoiseLevel] = useState(15);
    const [runsPerRoute, setRunsPerRoute] = useState(8);
    const [savedSimulations, setSavedSimulations] = useState([]);

    const [aiTrainingResult, setAiTrainingResult] = useState(null);
    const [aiTestPoints, setAiTestPoints] = useState([]);
    const [aiPrediction, setAiPrediction] = useState(null);

    useEffect(() => {
        const fontLink = document.createElement('link');
        fontLink.href = 'https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap';
        fontLink.rel = 'stylesheet';
        document.head.appendChild(fontLink);

        const initialMap = new Map({
            target: mapRef.current,
            layers: [
                new TileLayer({ source: new OSM() }),
                new VectorLayer({
                    source: sampleSource.current,
                    style: (feature) => {
                        const dir = feature.get('dir');
                        const color = dir === 'REVERSE' ? 'rgba(245, 158, 11, 0.6)' : 'rgba(14, 165, 233, 0.5)';
                        return new Style({
                            image: new Circle({
                                radius: 3.5,
                                fill: new Fill({ color }),
                                stroke: new Stroke({ color: 'rgba(255, 255, 255, 0.3)', width: 1 })
                            })
                        });
                    }
                }),
                new VectorLayer({
                    source: pathSource.current,
                    style: new Style({
                        stroke: new Stroke({ color: '#ef4444', width: 4, lineCap: 'round', lineJoin: 'round' })
                    })
                }),
                new VectorLayer({
                    source: medianSource.current,
                    style: new Style({
                        stroke: new Stroke({ color: '#22c55e', width: 5, lineCap: 'round', lineJoin: 'round' })
                    })
                }),
                new VectorLayer({
                    source: clickSource.current,
                    style: (feature) => new Style({
                        image: new Circle({
                            radius: 9,
                            fill: new Fill({ color: feature.get('type') === 'start' ? '#10b981' : '#f43f5e' }),
                            stroke: new Stroke({ color: '#ffffff', width: 2.5 })
                        })
                    })
                }),
                new VectorLayer({
                    source: aiTestSeqSource.current,
                    style: (feature) => {
                        if (feature.getGeometry().getType() === 'Point') {
                            return new Style({
                                image: new Circle({
                                    radius: 7,
                                    fill: new Fill({ color: '#a855f7' }),
                                    stroke: new Stroke({ color: '#ffffff', width: 2 })
                                })
                            });
                        } else {
                            return new Style({
                                stroke: new Stroke({ color: '#c084fc', width: 3, lineDash: [6, 6] })
                            });
                        }
                    }
                }),
                new VectorLayer({
                    source: aiMatchedRoadSource.current,
                    style: new Style({
                        stroke: new Stroke({ color: '#06b6d4', width: 8, lineCap: 'round', lineJoin: 'round' })
                    })
                }),
                new VectorLayer({
                    source: animVehicleSource.current,
                    style: (feature) => {
                        const dir = feature.get('dir');
                        const isFwd = dir === 'FORWARD';
                        return new Style({
                            image: new Circle({
                                radius: 7,
                                fill: new Fill({ color: isFwd ? '#38bdf8' : '#f59e0b' }),
                                stroke: new Stroke({ color: '#ffffff', width: 2.5 })
                            })
                        });
                    },
                    zIndex: 100
                })
            ],
            view: new View({
                center: [73.0479, 33.6844],
                zoom: 12,
                projection: 'EPSG:4326'
            })
        });

        initialMap.on('click', (e) => {
            const currentTab = activeTabRef.current;
            if (currentTab === 'ai_test') {
                setAiTestPoints((prev) => {
                    const updated = [...prev, e.coordinate];
                    drawAiTestSequence(updated);
                    return updated;
                });
                setStatusText('Added point to test sequence. Click \'Identify Road with AI\'.');
            } else {
                setClicks((prev) => {
                    const next = prev.length >= 2 ? [e.coordinate] : [...prev, e.coordinate];
                    drawMarkers(next);
                    return next;
                });
                resetPathAndPoints();
                setStatusText('Point added. Click again to set the destination.');
            }
        });

        setStatusText('Ready. Click two points on the map to plan a route.');
        loadSavedSimulations();

        return () => initialMap.setTarget(undefined);
    }, []);

    const activeTabRef = useRef(activeTab);
    useEffect(() => {
        activeTabRef.current = activeTab;
    }, [activeTab]);

    const drawAiTestSequence = (points) => {
        aiTestSeqSource.current.clear();
        points.forEach((pt, i) => {
            const feat = new Feature(new Point(pt));
            feat.set('index', i);
            aiTestSeqSource.current.addFeature(feat);
        });
        if (points.length > 1) {
            aiTestSeqSource.current.addFeature(new Feature(new LineString(points)));
        }
    };

    const clearAiTest = () => {
        setAiTestPoints([]);
        setAiPrediction(null);
        aiTestSeqSource.current.clear();
        aiMatchedRoadSource.current.clear();
        setStatusText('Cleared. Click points on the map to start a new test.');
    };

    const loadSavedSimulations = () => {
        fetch(`${API_BASE}/simulations`)
            .then((res) => res.ok ? res.json() : Promise.reject())
            .then(setSavedSimulations)
            .catch(() => setSavedSimulations([]));
    };

    const drawMarkers = (points) => {
        clickSource.current.clear();
        if (points[0]) {
            const start = new Feature(new Point(points[0]));
            start.set('type', 'start');
            clickSource.current.addFeature(start);
        }
        if (points[1]) {
            const end = new Feature(new Point(points[1]));
            end.set('type', 'end');
            clickSource.current.addFeature(end);
        }
    };

    const getPointAtProgress = (coords, progress) => {
        if (!coords || coords.length === 0) return null;
        if (coords.length === 1) return coords[0];
        const p = Math.max(0, Math.min(1, progress));

        let totalDist = 0;
        const dists = [0];
        for (let i = 1; i < coords.length; i++) {
            const dx = coords[i][0] - coords[i - 1][0];
            const dy = coords[i][1] - coords[i - 1][1];
            totalDist += Math.hypot(dx, dy);
            dists.push(totalDist);
        }
        if (totalDist === 0) return coords[0];

        const targetDist = p * totalDist;
        for (let i = 0; i < dists.length - 1; i++) {
            if (targetDist <= dists[i + 1]) {
                const segLen = dists[i + 1] - dists[i];
                const t = segLen > 0 ? (targetDist - dists[i]) / segLen : 0;
                return [
                    coords[i][0] + t * (coords[i + 1][0] - coords[i][0]),
                    coords[i][1] + t * (coords[i + 1][1] - coords[i][1])
                ];
            }
        }
        return coords[coords.length - 1];
    };

    const stopVehicleAnimation = () => {
        if (animFrameId.current) {
            cancelAnimationFrame(animFrameId.current);
            animFrameId.current = null;
        }
        animVehicleSource.current.clear();
        setIsSimulating(false);
    };

    const startVehicleAnimation = (coords = currentPathCoords.current, dir = routeDirection) => {
        if (!coords || coords.length < 2) {
            setStatusText('Calculate or load a route first before starting simulation.');
            return;
        }
        stopVehicleAnimation();
        setIsSimulating(true);

        const DURATION = 6500;
        animStartTime.current = performance.now();

        const animate = (time) => {
            const elapsed = time - animStartTime.current;
            animVehicleSource.current.clear();
            const features = [];

            if (dir === 'FORWARD') {
                const count = 4;
                for (let i = 0; i < count; i++) {
                    const prog = ((elapsed / DURATION) + (i / count)) % 1.0;
                    const pt = getPointAtProgress(coords, prog);
                    if (pt) {
                        const feat = new Feature(new Point(pt));
                        feat.set('dir', 'FORWARD');
                        features.push(feat);
                    }
                }
            } else if (dir === 'REVERSE') {
                const count = 4;
                for (let i = 0; i < count; i++) {
                    const prog = ((elapsed / DURATION) + (i / count)) % 1.0;
                    const pt = getPointAtProgress(coords, 1.0 - prog);
                    if (pt) {
                        const feat = new Feature(new Point(pt));
                        feat.set('dir', 'REVERSE');
                        features.push(feat);
                    }
                }
            } else {
                const count = 3;
                for (let i = 0; i < count; i++) {
                    const prog = ((elapsed / DURATION) + (i / count)) % 1.0;
                    const ptFwd = getPointAtProgress(coords, prog);
                    if (ptFwd) {
                        const f = new Feature(new Point(ptFwd));
                        f.set('dir', 'FORWARD');
                        features.push(f);
                    }
                    const ptRev = getPointAtProgress(coords, 1.0 - prog);
                    if (ptRev) {
                        const r = new Feature(new Point(ptRev));
                        r.set('dir', 'REVERSE');
                        features.push(r);
                    }
                }
            }

            animVehicleSource.current.addFeatures(features);
            animFrameId.current = requestAnimationFrame(animate);
        };

        animFrameId.current = requestAnimationFrame(animate);
    };

    const toggleSimulateVehicles = () => {
        if (isSimulating) {
            stopVehicleAnimation();
            setStatusText('Vehicle movement simulation paused.');
        } else {
            startVehicleAnimation(currentPathCoords.current, routeDirection);
            setStatusText(`Simulating: ${routeDirection === 'BOTH' ? 'Both Directions' : routeDirection === 'REVERSE' ? 'B → A' : 'A → B'}.`);
        }
    };

    const swapPoints = () => {
        if (clicks.length < 2) return;
        const swapped = [clicks[1], clicks[0]];
        setClicks(swapped);
        drawMarkers(swapped);
        calculatePath(swapped[0], swapped[1]);
        setStatusText('Swapped Start and End points. Recalculated route.');
    };

    useEffect(() => {
        if (isSimulating && currentPathCoords.current.length >= 2) {
            startVehicleAnimation(currentPathCoords.current, routeDirection);
        }
    }, [routeDirection]);

    const resetPathAndPoints = () => {
        stopVehicleAnimation();
        pathSource.current.clear();
        sampleSource.current.clear();
        medianSource.current.clear();
        currentPathCoords.current = [];
        setPathReady(false);
        setPathLength(null);
    };

    const calculatePath = (start = clicks[0], end = clicks[1], onReady) => {
        if (!start || !end) {
            setStatusText('Please select both a start and an end point.');
            return;
        }

        setLoading(true);
        setStatusText('Calculating shortest route via Islamabad road network...');
        fetch(`${API_BASE}/path?startX=${start[0]}&startY=${start[1]}&endX=${end[0]}&endY=${end[1]}`)
            .then((res) => res.json())
            .then((data) => {
                if (!data || data.length < 2) {
                    setStatusText('No road path found between selected points.');
                    return;
                }
                const pathCoords = data.map((c) => [c.x, c.y]);
                currentPathCoords.current = pathCoords;
                pathSource.current.clear();
                pathSource.current.addFeature(new Feature(new LineString(pathCoords)));

                let distSum = 0;
                for (let i = 1; i < pathCoords.length; i++) {
                    const dx = pathCoords[i][0] - pathCoords[i - 1][0];
                    const dy = pathCoords[i][1] - pathCoords[i - 1][1];
                    distSum += Math.hypot(dx, dy);
                }
                setPathLength((distSum * 111).toFixed(2));
                setPathReady(true);
                setStatusText('Route found! Pick a direction and save it, or test vehicle movement.');
                if (onReady) onReady();
            })
            .catch(() => setStatusText('Error calculating route.'))
            .finally(() => setLoading(false));
    };

    const saveRoute = () => {
        if (!pathReady || clicks.length < 2) {
            setStatusText('Find a route on the map before saving.');
            return;
        }
        if (!routeName.trim()) {
            setStatusText('Give this route a name first.');
            return;
        }

        setLoading(true);
        fetch(`${API_BASE}/simulations`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name: routeName.trim(),
                direction: routeDirection,
                startPoint: { x: clicks[0][0], y: clicks[0][1] },
                endPoint: { x: clicks[1][0], y: clicks[1][1] },
                vehicles: [{ averageSpeed: 60, speedNoise: 15, startDelaySeconds: 0, timeNoiseSeconds: 3, xCoordinateNoise: 0.0002, yCoordinateNoise: 0.0002 }]
            })
        })
            .then(async (res) => {
                if (!res.ok) {
                    const error = await res.json().catch(() => ({}));
                    throw new Error(error.message || error.error || 'Save route failed.');
                }
                return res.json();
            })
            .then((saved) => {
                setStatusText(`Route "${saved.name}" saved! Head to Train Model to generate simulation data.`);
                setRouteName('');
                loadSavedSimulations();
            })
            .catch((err) => setStatusText(err.message.includes('exists') ? 'A route with that name already exists.' : err.message))
            .finally(() => setLoading(false));
    };

    const generateRoutePoints = (sim, customDir = null) => {
        setLoading(true);
        const targetDir = customDir || (genDirection === 'ROUTE_DEFAULT' ? (sim.direction || 'BOTH') : genDirection);
        setStatusText(`Generating ${targetDir} organic drift trajectories for "${sim.name}"...`);
        fetch(`${API_BASE}/ai/generate-route-points/${sim.id}?numRuns=${runsPerRoute}&noiseLevel=${noiseLevel}&direction=${targetDir}`, {
            method: 'POST'
        })
            .then((res) => res.json())
            .then((data) => {
                setStatusText(`Generated ${data.generatedPoints} simulation points (${data.direction || targetDir}) for "${sim.name}". Saved to DB.`);
                loadSavedSimulations();
                viewSavedPoints(sim);
            })
            .catch((err) => setStatusText('Error generating route points: ' + err.message))
            .finally(() => setLoading(false));
    };

    const generateAllRoutesPoints = () => {
        setLoading(true);
        const dirParam = genDirection === 'ROUTE_DEFAULT' ? '' : `&direction=${genDirection}`;
        setStatusText(`Generating organic trajectories for ALL saved user routes (${genDirection === 'ROUTE_DEFAULT' ? 'per-route direction' : genDirection})...`);
        fetch(`${API_BASE}/ai/generate-all-routes-points?numRunsPerRoute=${runsPerRoute}&noiseLevel=${noiseLevel}${dirParam}`, {
            method: 'POST'
        })
            .then((res) => res.json())
            .then((data) => {
                setStatusText(`Generated ${data.totalGeneratedPoints} simulation points across ${data.totalSavedRoutes} user routes. Saved to DB.`);
                loadSavedSimulations();
            })
            .catch((err) => setStatusText('Error generating points: ' + err.message))
            .finally(() => setLoading(false));
    };

    const trainAiModel = () => {
        setLoading(true);
        setStatusText('Training AI Model on user-saved routes with bidirectional movement...');
        fetch(`${API_BASE}/ai/train`, { method: 'POST' })
            .then((res) => res.json())
            .then((data) => {
                setAiTrainingResult(data);
                setStatusText(`AI Model Trained! Accuracy: ${(data.accuracy * 100).toFixed(1)}% | Learned Routes: ${data.num_classes - 1} | Forward Samples: ${data.forward_samples || 0} | Reverse Samples: ${data.reverse_samples || 0}`);
            })
            .catch((err) => setStatusText('AI Training failed: ' + err.message))
            .finally(() => setLoading(false));
    };

    const predictAiSequence = () => {
        if (aiTestPoints.length === 0) {
            setStatusText('Please click points on map first to create a test sequence.');
            return;
        }

        setLoading(true);
        setStatusText('Evaluating sequence with AI Spatial & Directional Model...');
        const payload = aiTestPoints.map((pt) => ({ x: pt[0], y: pt[1] }));

        fetch(`${API_BASE}/ai/predict`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then((res) => res.json())
            .then((data) => {
                setAiPrediction(data);
                aiMatchedRoadSource.current.clear();

                if (data.is_on_road && data.matchedRoadGeom && data.matchedRoadGeom.coordinates) {
                    const lineCoords = data.matchedRoadGeom.coordinates.map((c) => [c.x, c.y]);
                    aiMatchedRoadSource.current.addFeature(new Feature(new LineString(lineCoords)));
                    const dirText = data.direction_label ? ` [${data.direction_label}]` : '';
                    setStatusText(`AI Prediction: MATCHED ROUTE "${data.route_name}"${dirText} (Confidence: ${(data.confidence * 100).toFixed(1)}%)`);
                } else {
                    setStatusText(`AI Prediction: NOT ON ANY ROUTE (Off-Road Sequence, Confidence: ${(data.confidence * 100).toFixed(1)}%)`);
                }
            })
            .catch((err) => setStatusText('AI Prediction failed: ' + err.message))
            .finally(() => setLoading(false));
    };

    const viewSavedPoints = (sim) => {
        sampleSource.current.clear();
        stopVehicleAnimation();
        if (!sim.points?.length) {
            setStatusText(`"${sim.name}" has no saved simulation points yet. Click "Points" to generate.`);
            return;
        }
        sampleSource.current.addFeatures(sim.points.map((p) => {
            const feat = new Feature(new Point([p.coordinate.x, p.coordinate.y]));
            feat.set('dir', p.direction || 'FORWARD');
            return feat;
        }));
        const fwdCount = sim.points.filter((p) => p.direction === 'FORWARD').length;
        const revCount = sim.points.filter((p) => p.direction === 'REVERSE').length;
        setStatusText(`Visualizing ${sim.points.length} points for route "${sim.name}" (Forward: ${fwdCount}, Reverse: ${revCount}).`);
    };

    const loadRouteForSimulation = (sim) => {
        const start = [sim.startPoint.x, sim.startPoint.y];
        const end = [sim.endPoint.x, sim.endPoint.y];
        setClicks([start, end]);
        drawMarkers([start, end]);
        setRouteDirection(sim.direction || 'BOTH');
        calculatePath(start, end, () => {
            setStatusText(`Loaded route "${sim.name}" (${sim.direction || 'BOTH'}). Click "Simulate Vehicle Movement" to view.`);
        });
    };

    const deleteSavedSimulation = (simulation) => {
        fetch(`${API_BASE}/simulations/${simulation.id}`, { method: 'DELETE' })
            .then((res) => {
                if (!res.ok) throw new Error('Delete failed.');
                setStatusText(`Deleted route "${simulation.name}".`);
                loadSavedSimulations();
            })
            .catch((err) => setStatusText(err.message));
    };

    const clearSelection = () => {
        clickSource.current.clear();
        resetPathAndPoints();
        setClicks([]);
        setRouteName('');
        setStatusText('Selection cleared. Place Start and End points on the map.');
    };

    const panel = {
        position: 'absolute',
        top: '20px',
        left: '20px',
        width: '460px',
        maxHeight: 'calc(100vh - 40px)',
        overflowY: 'auto',
        background: 'rgba(15, 23, 42, 0.93)',
        backdropFilter: 'blur(16px)',
        borderRadius: '10px',
        padding: '22px',
        color: '#f8fafc',
        zIndex: 1000,
        border: '1px solid rgba(255, 255, 255, 0.12)',
        boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4)'
    };

    const tabButtonStyle = (active) => ({
        flex: 1,
        padding: '8px 10px',
        borderRadius: '6px',
        border: 'none',
        fontSize: '12px',
        fontWeight: 600,
        background: active ? '#2563eb' : 'rgba(30, 41, 59, 0.8)',
        color: active ? '#ffffff' : '#94a3b8',
        cursor: 'pointer',
        transition: 'all 0.2s'
    });

    const inputStyle = {
        width: '100%',
        boxSizing: 'border-box',
        border: '1px solid rgba(148, 163, 184, 0.35)',
        borderRadius: '6px',
        background: 'rgba(15, 23, 42, 0.7)',
        color: '#f8fafc',
        padding: '9px 10px',
        outline: 'none'
    };

    const buttonStyle = (variant = 'primary', disabled = false) => ({
        padding: '10px 14px',
        borderRadius: '6px',
        border: variant === 'ghost' ? '1px solid rgba(148, 163, 184, 0.35)' : 'none',
        background: disabled
            ? '#334155'
            : variant === 'danger'
                ? '#be123c'
                : variant === 'amber'
                    ? '#d97706'
                    : variant === 'purple'
                        ? '#9333ea'
                        : variant === 'cyan'
                            ? '#0891b2'
                            : variant === 'ghost'
                                ? '#1e293b'
                                : '#2563eb',
        color: disabled ? '#94a3b8' : '#ffffff',
        fontWeight: 600,
        cursor: disabled ? 'not-allowed' : 'pointer'
    });

    const dirButtonStyle = (active, color = '#2563eb') => ({
        flex: 1,
        padding: '8px 4px',
        borderRadius: '6px',
        border: active ? `2px solid #93c5fd` : '1px solid rgba(148, 163, 184, 0.3)',
        fontSize: '11px',
        fontWeight: 700,
        background: active ? color : 'rgba(30, 41, 59, 0.7)',
        color: active ? '#ffffff' : '#cbd5e1',
        cursor: 'pointer',
        transition: 'all 0.15s'
    });

    const sectionStyle = {
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        paddingTop: '14px',
        marginTop: '14px',
        borderTop: '1px solid rgba(148, 163, 184, 0.18)'
    };

    return (
        <div style={{ fontFamily: "'Outfit', sans-serif", height: '100vh', position: 'relative', overflow: 'hidden' }}>
            <div ref={mapRef} style={{ width: '100%', height: '100%', cursor: 'crosshair' }} />

            <div style={panel}>
                <h2 style={{ margin: 0, fontSize: '21px', fontWeight: 700, color: '#60a5fa' }}>Islamabad Road Navigation</h2>

                <div style={{ display: 'flex', gap: '6px', marginTop: '14px', background: 'rgba(30, 41, 59, 0.6)', padding: '4px', borderRadius: '8px' }}>
                    <button style={tabButtonStyle(activeTab === 'create_route')} onClick={() => setActiveTab('create_route')}>Routes</button>
                    <button style={tabButtonStyle(activeTab === 'ai_train')} onClick={() => setActiveTab('ai_train')}>Train Model</button>
                    <button style={tabButtonStyle(activeTab === 'ai_test')} onClick={() => setActiveTab('ai_test')}>Test Sequence</button>
                </div>

                <div style={{
                    marginTop: '12px',
                    background: 'rgba(30, 41, 59, 0.7)',
                    borderLeft: '3px solid #3b82f6',
                    borderRadius: '6px',
                    padding: '10px',
                    fontSize: '13px',
                    lineHeight: 1.4
                }}>
                    {statusText}
                </div>

                {activeTab === 'create_route' && (
                    <>
                        <div style={sectionStyle}>
                            <h3 style={{ margin: 0, fontSize: '15px', color: '#60a5fa' }}>Set Start & End Points</h3>
                            <InfoRow label="Start (A)" value={clicks[0] ? `${clicks[0][1].toFixed(4)}, ${clicks[0][0].toFixed(4)}` : 'Click map to set'} />
                            <InfoRow label="End (B)" value={clicks[1] ? `${clicks[1][1].toFixed(4)}, ${clicks[1][0].toFixed(4)}` : 'Click map to set'} />
                            {pathLength && <InfoRow label="Route Distance" value={`${pathLength} km`} highlight />}
                            <div style={{ display: 'flex', gap: '8px' }}>
                                <button onClick={() => calculatePath()} disabled={loading || clicks.length < 2} style={buttonStyle('primary', loading || clicks.length < 2)}>
                                    {loading ? 'Working...' : 'Find Route'}
                                </button>
                                <button onClick={swapPoints} disabled={clicks.length < 2} style={buttonStyle('ghost', clicks.length < 2)}>
                                    Swap (A ⇄ B)
                                </button>
                                <button onClick={clearSelection} style={buttonStyle('ghost')}>Clear</button>
                            </div>
                        </div>

                        {pathReady && (
                            <div style={sectionStyle}>
                                <h3 style={{ margin: 0, fontSize: '15px', color: '#38bdf8' }}>Direction & Simulation</h3>
                                <div style={{ display: 'flex', gap: '6px' }}>
                                    <button
                                        type="button"
                                        onClick={() => setRouteDirection('FORWARD')}
                                        style={dirButtonStyle(routeDirection === 'FORWARD', '#2563eb')}
                                    >
                                        Forward (A → B)
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setRouteDirection('REVERSE')}
                                        style={dirButtonStyle(routeDirection === 'REVERSE', '#d97706')}
                                    >
                                        Reverse (B → A)
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setRouteDirection('BOTH')}
                                        style={dirButtonStyle(routeDirection === 'BOTH', '#7c3aed')}
                                    >
                                        Both Directions (A ⇄ B)
                                    </button>
                                </div>

                                <button
                                    onClick={toggleSimulateVehicles}
                                    style={buttonStyle(isSimulating ? 'amber' : 'purple')}
                                >
                                    {isSimulating ? '⏸ Pause Vehicle Simulation' : '▶ Simulate Vehicle Movement on Map'}
                                </button>
                            </div>
                        )}

                        {pathReady && (
                            <div style={sectionStyle}>
                                <h3 style={{ margin: 0, fontSize: '15px', color: '#f59e0b' }}>Name & Save Your Route</h3>
                                <InfoRow
                                    label="Configured Flow"
                                    value={routeDirection === 'BOTH' ? 'Both Directions (A ⇄ B)' : routeDirection === 'REVERSE' ? 'Reverse (B → A)' : 'Forward (A → B)'}
                                    highlight
                                />
                                <input
                                    style={inputStyle}
                                    value={routeName}
                                    onChange={(e) => setRouteName(e.target.value)}
                                    placeholder='Route name'
                                />
                                <button onClick={saveRoute} disabled={loading} style={buttonStyle('amber', loading)}>Save Named Route</button>
                            </div>
                        )}

                        <div style={sectionStyle}>
                            <h3 style={{ margin: 0, fontSize: '15px', color: '#60a5fa' }}>Saved User Routes</h3>
                            {savedSimulations.filter(s => s.name !== 'Off-Road Noise Dataset').length === 0 ? (
                                <div style={{ fontSize: '13px', color: '#94a3b8' }}>No saved routes yet. Select points on map and save a route!</div>
                            ) : savedSimulations.filter(s => s.name !== 'Off-Road Noise Dataset').map((sim) => (
                                <div key={sim.id} style={{
                                    display: 'grid',
                                    gridTemplateColumns: '1fr auto auto auto auto',
                                    alignItems: 'center',
                                    gap: '6px',
                                    background: 'rgba(30, 41, 59, 0.55)',
                                    borderRadius: '6px',
                                    padding: '9px'
                                }}>
                                    <div>
                                        <div style={{ fontWeight: 700, color: '#f8fafc' }}>{sim.name}</div>
                                        <div style={{ display: 'flex', gap: '6px', alignItems: 'center', marginTop: '3px' }}>
                                            <span style={{
                                                fontSize: '10px',
                                                fontWeight: 700,
                                                padding: '2px 6px',
                                                borderRadius: '4px',
                                                background: sim.direction === 'BOTH' ? 'rgba(124, 58, 237, 0.35)' : sim.direction === 'REVERSE' ? 'rgba(217, 119, 6, 0.35)' : 'rgba(37, 99, 235, 0.35)',
                                                color: sim.direction === 'BOTH' ? '#c084fc' : sim.direction === 'REVERSE' ? '#fbbf24' : '#60a5fa',
                                                border: `1px solid ${sim.direction === 'BOTH' ? 'rgba(124, 58, 237, 0.6)' : sim.direction === 'REVERSE' ? 'rgba(217, 119, 6, 0.6)' : 'rgba(37, 99, 235, 0.6)'}`
                                            }}>
                                                {sim.direction === 'BOTH' ? 'Both (A ⇄ B)' : sim.direction === 'REVERSE' ? 'Reverse (B → A)' : 'Forward (A → B)'}
                                            </span>
                                            <span style={{ fontSize: '11px', color: '#94a3b8' }}>
                                                {sim.points?.length || 0} pts
                                            </span>
                                        </div>
                                    </div>
                                    <button title="Simulate vehicle movement on map" onClick={() => loadRouteForSimulation(sim)} style={buttonStyle('purple')}>Sim</button>
                                    <button title="Generate noisy organic points" onClick={() => generateRoutePoints(sim)} disabled={loading} style={buttonStyle('primary', loading)}>Points</button>
                                    <button title="View points on map" onClick={() => viewSavedPoints(sim)} style={buttonStyle('ghost')}>View</button>
                                    <button title="Delete route" onClick={() => deleteSavedSimulation(sim)} style={buttonStyle('danger')}>Del</button>
                                </div>
                            ))}
                        </div>
                    </>
                )}

                {activeTab === 'ai_train' && (
                    <>
                        <div style={sectionStyle}>
                            <h3 style={{ margin: 0, fontSize: '15px', color: '#a855f7' }}>Generate Training Points</h3>
                            <NumberControl label="Runs per Route" value={runsPerRoute} min="1" step="1" onChange={setRunsPerRoute} />
                            <NumberControl label="Noise Level (%)" value={noiseLevel} min="1" step="1" onChange={setNoiseLevel} />

                            <label style={{ display: 'grid', gridTemplateColumns: '1fr 160px', gap: '10px', alignItems: 'center', fontSize: '12px', color: '#cbd5e1' }}>
                                <span>Generation Direction</span>
                                <select
                                    value={genDirection}
                                    onChange={(e) => setGenDirection(e.target.value)}
                                    style={{
                                        width: '100%',
                                        boxSizing: 'border-box',
                                        border: '1px solid rgba(148, 163, 184, 0.35)',
                                        borderRadius: '6px',
                                        background: 'rgba(15, 23, 42, 0.7)',
                                        color: '#f8fafc',
                                        padding: '7px 8px',
                                        outline: 'none'
                                    }}
                                >
                                    <option value="ROUTE_DEFAULT">Use Route Setting</option>
                                    <option value="BOTH">Both Directions (50/50)</option>
                                    <option value="FORWARD">Forward Only (A → B)</option>
                                    <option value="REVERSE">Reverse Only (B → A)</option>
                                </select>
                            </label>

                            <button onClick={generateAllRoutesPoints} disabled={loading} style={buttonStyle('purple', loading)}>
                                {loading ? 'Generating...' : 'Generate Points for All Saved Routes'}
                            </button>
                        </div>

                        <div style={sectionStyle}>
                            <h3 style={{ margin: 0, fontSize: '15px', color: '#06b6d4' }}>Train the Model</h3>
                            <button onClick={trainAiModel} disabled={loading} style={buttonStyle('cyan', loading)}>
                                {loading ? 'Training Model...' : 'Train AI Model on Saved Routes'}
                            </button>

                            {aiTrainingResult && (
                                <div style={{
                                    background: 'rgba(6, 182, 212, 0.15)',
                                    border: '1px solid rgba(6, 182, 212, 0.4)',
                                    borderRadius: '8px',
                                    padding: '12px',
                                    marginTop: '10px'
                                }}>
                                    <div style={{ fontWeight: 700, color: '#38bdf8', marginBottom: '6px' }}>Training Result</div>
                                    <InfoRow label="Accuracy" value={`${(aiTrainingResult.accuracy * 100).toFixed(1)}%`} highlight />
                                    <InfoRow label="Total Trajectory Samples" value={aiTrainingResult.total_samples} />
                                    <InfoRow label="Forward Samples" value={aiTrainingResult.forward_samples || 0} />
                                    <InfoRow label="Reverse Samples" value={aiTrainingResult.reverse_samples || 0} />
                                    <InfoRow label="Learned Route Classes" value={aiTrainingResult.num_classes} />
                                    <div style={{ marginTop: '8px', fontSize: '12px', color: '#cbd5e1' }}>
                                        <strong>Learned Routes:</strong>
                                        <ul style={{ margin: '4px 0 0 16px', padding: 0 }}>
                                            {Object.entries(aiTrainingResult.route_names || {}).map(([id, name]) => (
                                                <li key={id}>{name} (Class {id})</li>
                                            ))}
                                        </ul>
                                    </div>
                                </div>
                            )}
                        </div>
                    </>
                )}

                {activeTab === 'ai_test' && (
                    <>
                        <div style={sectionStyle}>
                            <h3 style={{ margin: 0, fontSize: '15px', color: '#ec4899' }}>Test a Point Sequence</h3>
                            <p style={{ margin: 0, fontSize: '12px', color: '#94a3b8' }}>
                                Click 3–5 points on the map. The model will check which route they match and which direction you're traveling.
                            </p>
                            <InfoRow label="Points Selected" value={aiTestPoints.length} highlight />

                            <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                                <button onClick={predictAiSequence} disabled={loading || aiTestPoints.length === 0} style={buttonStyle('primary', loading || aiTestPoints.length === 0)}>
                                    {loading ? 'Checking...' : 'Identify Route & Direction'}
                                </button>
                                <button onClick={clearAiTest} style={buttonStyle('ghost')}>Reset</button>
                            </div>
                        </div>

                        {aiPrediction && (
                            <div style={{
                                background: aiPrediction.is_on_road ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                                border: `1px solid ${aiPrediction.is_on_road ? 'rgba(16, 185, 129, 0.4)' : 'rgba(239, 68, 68, 0.4)'}`,
                                borderRadius: '8px',
                                padding: '14px',
                                marginTop: '14px'
                            }}>
                                <div style={{
                                    fontSize: '16px',
                                    fontWeight: 700,
                                    color: aiPrediction.is_on_road ? '#34d399' : '#f87171',
                                    marginBottom: '8px'
                                }}>
                                    {aiPrediction.is_on_road ? `MATCHED ROUTE: "${aiPrediction.route_name}"` : 'NOT ON ANY ROUTE'}
                                </div>

                                <InfoRow label="Route Name" value={aiPrediction.route_name} highlight />
                                <InfoRow label="Route ID" value={aiPrediction.route_id === 0 ? 'None (Off-Road)' : aiPrediction.route_id} />
                                <InfoRow label="AI Confidence" value={`${(aiPrediction.confidence * 100).toFixed(1)}%`} highlight />

                                {aiPrediction.is_on_road && (
                                    <>
                                        <div style={{ marginTop: '10px', paddingTop: '8px', borderTop: '1px solid rgba(255, 255, 255, 0.1)' }}>
                                            <div style={{ fontSize: '13px', fontWeight: 700, color: aiPrediction.direction === 'REVERSE' ? '#fbbf24' : '#38bdf8', marginBottom: '6px' }}>
                                                Detected Vehicle Movement: {aiPrediction.direction_label || aiPrediction.direction}
                                            </div>
                                            {aiPrediction.direction_confidence && (
                                                <InfoRow label="Direction Confidence" value={`${(aiPrediction.direction_confidence * 100).toFixed(1)}%`} highlight />
                                            )}
                                            {aiPrediction.route_direction && (
                                                <InfoRow label="Route Capability" value={aiPrediction.route_direction === 'BOTH' ? 'Bidirectional (Learned Both Ways)' : aiPrediction.route_direction} />
                                            )}
                                        </div>

                                        {aiPrediction.direction_probabilities && (
                                            <div style={{ marginTop: '8px', fontSize: '11px', color: '#cbd5e1' }}>
                                                <strong>Direction Probabilities:</strong>
                                                {Object.entries(aiPrediction.direction_probabilities).map(([dir, prob]) => (
                                                    <div key={dir} style={{ display: 'flex', justifyContent: 'space-between', marginTop: '2px' }}>
                                                        <span>{dir}</span>
                                                        <span>{(prob * 100).toFixed(1)}%</span>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </>
                                )}

                                {aiPrediction.probabilities && (
                                    <div style={{ marginTop: '10px', fontSize: '11px', color: '#cbd5e1' }}>
                                        <strong>Class Probabilities:</strong>
                                        {Object.entries(aiPrediction.probabilities).map(([name, prob]) => (
                                            <div key={name} style={{ display: 'flex', justifyContent: 'space-between', marginTop: '2px' }}>
                                                <span>{name}</span>
                                                <span>{(prob * 100).toFixed(1)}%</span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </>
                )}
            </div>

            <div style={{
                position: 'absolute',
                bottom: '16px',
                right: '16px',
                background: 'rgba(15, 23, 42, 0.75)',
                padding: '6px 12px',
                borderRadius: '6px',
                fontSize: '11px',
                color: '#94a3b8',
                zIndex: 1000,
                pointerEvents: 'none'
            }}>
                Map Tiles &copy; OpenStreetMap
            </div>
        </div>
    );
}

function InfoRow({ label, value, highlight = false }) {
    return (
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '10px', fontSize: '13px' }}>
            <span style={{ color: highlight ? '#60a5fa' : '#94a3b8', fontWeight: highlight ? 700 : 400 }}>{label}</span>
            <span style={{ color: highlight ? '#60a5fa' : '#f8fafc', fontFamily: 'monospace', textAlign: 'right' }}>{value}</span>
        </div>
    );
}

function NumberControl({ label, value, min, step, onChange }) {
    return (
        <label style={{ display: 'grid', gridTemplateColumns: '1fr 128px', gap: '10px', alignItems: 'center', fontSize: '12px', color: '#cbd5e1' }}>
            <span>{label}</span>
            <input
                type="number"
                min={min}
                step={step}
                value={value}
                onChange={(e) => onChange(Number(e.target.value))}
                style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    border: '1px solid rgba(148, 163, 184, 0.35)',
                    borderRadius: '6px',
                    background: 'rgba(15, 23, 42, 0.7)',
                    color: '#f8fafc',
                    padding: '7px 8px',
                    outline: 'none'
                }}
            />
        </label>
    );
}

export default App;
