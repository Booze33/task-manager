CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    role VARCHAR(20) NOT NULL,
    failed_login_attempts INTEGER NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             CONSTRAINT uk_users_email UNIQUE (email)
    );

CREATE TABLE IF NOT EXISTS tasks (
                                     id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
                               CONSTRAINT fk_tasks_owner FOREIGN KEY (owner_id) REFERENCES users (id)
    );

CREATE INDEX IF NOT EXISTS idx_tasks_owner_created ON tasks (owner_id, created_at, id);
CREATE INDEX IF NOT EXISTS idx_tasks_owner_status ON tasks (owner_id, status);
