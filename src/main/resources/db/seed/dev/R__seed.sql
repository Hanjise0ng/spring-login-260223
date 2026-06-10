DELETE FROM devices;
DELETE FROM social_accounts;
DELETE FROM users;

INSERT INTO users (public_id, login_id, password, nickname, tag, email, role, auth_provider, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'admin1234',
     '$2a$10$UeP9roeoADKdfzkkE59FteHkPRAWHXt3Q8PtZZJEzkRbWl/amOg5O', -- pw: admin1234!
     '개발관리자', 'D001', 'admin@dev.test', 'ADMIN', 'LOCAL', NOW(6), NOW(6)),
    ('00000000-0000-0000-0000-0000000000a2', 'user1234',
     '$2a$10$9yBo./TFZxRp7FtlLi3QB.G39DHn5u4Fnqcgo7Ln.x7IAJbmo0jli', -- pw: user1234!
     '개발유저', 'D002', 'user@dev.test', 'USER', 'LOCAL', NOW(6), NOW(6));