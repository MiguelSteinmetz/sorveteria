-- As senhas passam a ser guardadas com BCrypt.
-- Converte as senhas que ainda estao em texto puro para hash BCrypt ($2a$),
-- que e compativel com o BCryptPasswordEncoder do Spring Security.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE usuarios
SET senha = crypt(senha, gen_salt('bf', 10))
WHERE senha NOT LIKE '$2a$%'
  AND senha NOT LIKE '$2b$%'
  AND senha NOT LIKE '$2y$%';
