CREATE TABLE IF NOT EXISTS code_submissions (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    code_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    user_id INT NOT NULL
);