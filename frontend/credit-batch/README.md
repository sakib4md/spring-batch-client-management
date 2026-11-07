# Credit Batch UI

A responsive React dashboard for viewing, filtering, sorting, and importing credit-client CSV data.

## Features

- Loads clients from the Spring Boot API
- Uploads CSV files to the Java import API
- Filters by country, status, and rating
- Sorts table columns and paginates results
- Search by client, industry, country, rating, package, or status
- Shows client, score, rating, revenue, exposure, and status summary
- Responsive desktop and mobile layout

## Requirements

- Node.js 18 or newer
- Credit Batch backend running on port 8080 for database data

## Run

```powershell
cd E:\frontend\credit-batch
npm install
npm run dev
```

Open `http://localhost:5173`.

You can also open the folder with a static server and upload a CSV using the **Upload CSV** button. The expected CSV columns are the same as `incoming/clients.csv` in the backend project.
