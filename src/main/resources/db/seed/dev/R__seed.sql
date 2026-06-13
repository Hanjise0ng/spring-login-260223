DELETE FROM devices;
DELETE FROM credentials;
DELETE FROM users;

INSERT INTO users (public_id, nickname, tag, email, role, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000a1',
     '개발관리자', 'D001', 'admin@dev.test', 'ADMIN', NOW(6), NOW(6)),
    ('00000000-0000-0000-0000-0000000000a2',
     '개발유저', 'D002', 'user@dev.test', 'USER', NOW(6), NOW(6));

INSERT INTO credentials (user_id, provider, identifier, password, created_at, updated_at)
VALUES
    ((SELECT id FROM users WHERE public_id = '00000000-0000-0000-0000-0000000000a1'),
     'LOCAL', 'admin1234',
     '$2a$10$UeP9roeoADKdfzkkE59FteHkPRAWHXt3Q8PtZZJEzkRbWl/amOg5O', -- pw: admin1234!
     NOW(6), NOW(6)),
    ((SELECT id FROM users WHERE public_id = '00000000-0000-0000-0000-0000000000a2'),
     'LOCAL', 'user1234',
     '$2a$10$9yBo./TFZxRp7FtlLi3QB.G39DHn5u4Fnqcgo7Ln.x7IAJbmo0jli', -- pw: user1234!
     NOW(6), NOW(6));