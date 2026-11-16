CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tickets (
                         id BIGSERIAL PRIMARY KEY,
                         title VARCHAR(200) NOT NULL,
                         description TEXT,
                         category_id BIGINT NOT NULL,
                         priority VARCHAR(20) NOT NULL,
                         status VARCHAR(20) NOT NULL DEFAULT 'NEW',
                         created_at TIMESTAMP NOT NULL,
                         due_at TIMESTAMP,
                         closed_at TIMESTAMP,

                         CONSTRAINT fk_ticket_category
                             FOREIGN KEY (category_id)
                                 REFERENCES categories(id),

                         CONSTRAINT chk_ticket_priority
                             CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),

                         CONSTRAINT chk_ticket_status
                             CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED'))
);

CREATE TABLE ticket_status_history (
                                       id BIGSERIAL PRIMARY KEY,
                                       ticket_id BIGINT NOT NULL,
                                       old_status VARCHAR(20),
                                       new_status VARCHAR(20) NOT NULL,
                                       changed_at TIMESTAMP NOT NULL,

                                       CONSTRAINT fk_history_ticket
                                           FOREIGN KEY (ticket_id)
                                               REFERENCES tickets(id)
);