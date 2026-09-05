# Islamabad Road Navigation

An interactive road-navigation application for Islamabad. Users can select two map points, calculate a route, save it, simulate vehicles travelling along it, generate noisy trajectory samples, and identify the route and direction of a test sequence.

## Project structure

| Directory | Purpose |
| --- | --- |
| `frontend/` | React single-page application and OpenLayers map UI. Runs on port `3000`. |
| `backend/` | Spring Boot REST API. Loads the road network, calculates paths, and persists routes and samples. Runs on port `8081`. |
| `ai_service/` | FastAPI service for training data storage and route/direction prediction. Runs on port `8000`. |
| `backend/src/main/resources/` | PostgreSQL/PostGIS schema and Islamabad road-network data. |

## How the application works

1. The React frontend displays OpenStreetMap tiles with OpenLayers and sends map actions to the Spring Boot API.
2. The backend reads the bundled Islamabad road data, builds a graph, and uses JGraphT to find a path between the selected start and end coordinates.
3. Saved routes, their endpoints, generated points, and vehicle settings are stored in PostgreSQL. PostGIS stores the geographic points as `geometry(Point, 4326)`.
4. The backend can create forward, reverse, or bidirectional noisy point sequences for each saved route, including an off-road sample set.
5. When **Train Model** is used, the backend sends saved route geometry and generated samples to the FastAPI service. The service stores the candidate-route geometry in `ai_service/user_route_classifier.joblib`.
6. For prediction, the FastAPI service projects every test point onto each candidate route, chooses the closest route, and infers forward/reverse travel from progress along its polyline. A sequence farther than the configured threshold is reported as off-road.

The Python component is named an AI service in the UI, but its current prediction method is deterministic geometry matching; it does not train a neural network or scikit-learn classifier.

## Technologies

| Area | Technology |
| --- | --- |
| Frontend | React `19.2.7`, React DOM `19.2.7`, Create React App / `react-scripts` `5.0.1`, OpenLayers `10.6.1` |
| Backend | Java `17`, Maven Wrapper `3.3.4` / Maven `3.9.16`, Spring Boot `3.2.5`, Spring Web, Spring Data JPA, Hibernate Spatial, Lombok |
| Routing | JGraphT `1.5.2` |
| Database | PostgreSQL `16` or `17` with the PostGIS extension |
| AI service | Python `3.13`, FastAPI, Uvicorn, NumPy, Joblib, Pydantic |
| Map data | OpenStreetMap tiles and bundled Islamabad road-network JSON |

### Required tooling

Install the following before starting the project:

- Git
- Node.js `20 LTS` with Corepack enabled
- Java Development Kit `17`
- PostgreSQL `16` or `17`, including PostGIS for the same major version
- Python `3.13`

Maven does not need to be installed separately: the included Maven Wrapper downloads the required Maven version on its first run.

## Clone the repository

```powershell
git clone <repository-url>
cd Roads-Navigation
```

Replace `<repository-url>` with the GitHub clone URL.

## Database setup

1. Install PostgreSQL and PostGIS. On Windows, install PostgreSQL first, then use Stack Builder to install the matching PostGIS package.
2. Open **SQL Shell (psql)** or a PostgreSQL terminal and create the database:

   ```sql
   CREATE DATABASE "RoadDB";
   ```

3. Connect to `RoadDB` and enable PostGIS:

   ```sql
   \c RoadDB
   CREATE EXTENSION IF NOT EXISTS postgis;
   ```

4. Configure the backend connection in [`backend/src/main/resources/application.properties`](backend/src/main/resources/application.properties):

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/RoadDB
   spring.datasource.username=YOUR_POSTGRES_USERNAME
   spring.datasource.password=YOUR_POSTGRES_PASSWORD
   ```

   Replace `YOUR_POSTGRES_USERNAME` and `YOUR_POSTGRES_PASSWORD` with the PostgreSQL credentials on the supervisor's machine. For a typical local PostgreSQL installation, the username is `postgres`; the password is the one chosen during PostgreSQL installation. The database name, host, or port can also be changed there if required.

5. The backend automatically runs [`database-setup.sql`](backend/src/main/resources/database-setup.sql) at startup. It creates the tables and indexes if they do not exist. The database account must be permitted to create/use the PostGIS extension; otherwise create the extension as a PostgreSQL administrator before starting the backend.

## Install dependencies

Run these commands once after cloning the repository.

### Frontend

```powershell
cd frontend
corepack enable
corepack npm install
```

### Backend

No separate dependency-install command is needed. The Maven Wrapper resolves Java dependencies the first time it runs.

```powershell
cd backend
.\mvnw.cmd dependency:go-offline
```

### AI service

```powershell
cd ai_service
py -3.13 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

If PowerShell prevents activation, use Command Prompt and run `.venv\Scripts\activate.bat`, or invoke `.venv\Scripts\python.exe` directly in the run command below.

## Run the complete project

Start PostgreSQL first. Then open three terminals at the repository root and run one service in each.

### Terminal 1 - frontend

```powershell
cd frontend
corepack npm start
```

Open [http://localhost:3000](http://localhost:3000).

### Terminal 2 - backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API is available at [http://localhost:8081/api](http://localhost:8081/api).

### Terminal 3 - AI service

```powershell
cd ai_service
.\.venv\Scripts\Activate.ps1
python app.py
```

Verify it at [http://localhost:8000/health](http://localhost:8000/health). FastAPI also exposes interactive API documentation at [http://localhost:8000/docs](http://localhost:8000/docs).

## Quick test flow

1. Visit the frontend and click two points on the map to calculate a route.
2. Choose the allowed direction, give the route a name, and save it.
3. Open **Train Model**, generate route points, then train the model.
4. Open **Test Sequence**, click several points near a saved route, and identify the matching route and direction.

## Useful checks

```powershell
# Backend API health by listing saved routes
Invoke-RestMethod http://localhost:8081/api/simulations

# AI service health
Invoke-RestMethod http://localhost:8000/health
```

## Notes for reviewers

- The frontend expects the backend on `http://localhost:8081/api`.
- The backend expects the AI service on `http://127.0.0.1:8000`.
- The AI service writes its generated model data to `ai_service/user_route_classifier.joblib`.
- Do not commit local virtual environments, `node_modules`, build output, or database credentials intended for a real deployment.
