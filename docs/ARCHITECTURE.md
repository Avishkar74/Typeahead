# Architecture Documentation

## Overview

The Search Typeahead System is a high-performance, distributed caching solution for providing real-time search suggestions. It consists of three main components: API Layer, Caching Layer, and Data Layer.

## System Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        UI["🖥️ Frontend UI<br/>(React)"]
    end
    
    subgraph "API Gateway"
        LB["Load Balancer"]
    end
    
    subgraph "Application Layer"
        API["⚙️ FastAPI Backend<br/>- GET /suggest<br/>- POST /search<br/>- GET /cache/debug"]
    end
    
    subgraph "Processing Layer"
        BatchMgr["📦 Batch Write Manager<br/>- In-Memory Buffer<br/>- Flush Logic<br/>- 30sec/10 items"]
        TimeMgr["⏰ Virtual Time Manager<br/>- Real Elapsed Time<br/>- 2006 Context<br/>- Persistence"]
    end
    
    subgraph "Cache Layer"
        Redis["🔴 Redis Cluster<br/>- Node 1 (6379)<br/>- Node 2 (6380)<br/>- Node 3 (6381)<br/>- Consistent Hashing"]
    end
    
    subgraph "Data Layer"
        PG["🐘 PostgreSQL<br/>- queries table<br/>- search_logs table<br/>- system_config table<br/>- batch_buffer table"]
    end
    
    UI -->|Type: 'iph'| LB
    LB -->|GET /suggest?q=iph| API
    API -->|Cache Hit?| Redis
    Redis -->|Hit: 100ms cache| API
    Redis -->|Miss| PG
    PG -->|Fetch from DB| API
    API -->|Store in cache| Redis
    API -->|JSON Response| LB
    LB -->|Suggestions| UI
    
    UI -->|Search: 'iphone'| LB
    LB -->|POST /search| API
    API -->|Add to buffer| BatchMgr
    BatchMgr -->|Every 30s OR 10 items| PG
    API -->|Get virtual time| TimeMgr
    TimeMgr -->|Elapsed time| API
    
    PG -->|Store config| TimeMgr
    BatchMgr -->|Invalidate prefixes| Redis
    
    style UI fill:#e1f5ff
    style API fill:#fff3e0
    style Redis fill:#ffebee
    style PG fill:#f3e5f5
    style BatchMgr fill:#e8f5e9
    style TimeMgr fill:#fce4ec
