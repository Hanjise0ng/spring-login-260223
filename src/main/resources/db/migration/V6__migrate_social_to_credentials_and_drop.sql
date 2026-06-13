INSERT INTO credentials (user_id, provider, identifier, password, created_at, updated_at)
SELECT user_id, provider, provider_id, NULL, created_at, updated_at
FROM social_accounts;

SELECT CASE
           WHEN EXISTS (SELECT 1
                        FROM social_accounts sa
                                 LEFT JOIN credentials c
                                           ON c.provider = sa.provider
                                               AND c.identifier = sa.provider_id
                        WHERE c.id IS NULL)
               THEN 1 / 0
           ELSE 1
           END AS migration_integrity_check;

DROP TABLE social_accounts;

ALTER TABLE users DROP COLUMN auth_provider;