-- =============================================
-- 전체 서비스 데이터베이스 및 사용자 생성
-- MySQL 단일 인스턴스에 논리적으로 분리된 DB 구성
-- =============================================

SET NAMES utf8mb4;

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS user_service_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS problem_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS learning_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS media_service_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'user_service_user'@'%'    IDENTIFIED BY 'user_service_password';
CREATE USER IF NOT EXISTS 'problem_service_user'@'%' IDENTIFIED BY 'problem_service_password';
CREATE USER IF NOT EXISTS 'learning_user'@'%'        IDENTIFIED BY '1234';
CREATE USER IF NOT EXISTS 'media_service_user'@'%'   IDENTIFIED BY 'media_service_password';

GRANT ALL PRIVILEGES ON user_service_db.*     TO 'user_service_user'@'%';
GRANT ALL PRIVILEGES ON problem_service_db.*  TO 'problem_service_user'@'%';
GRANT ALL PRIVILEGES ON learning_service_db.* TO 'learning_user'@'%';
GRANT ALL PRIVILEGES ON media_service_db.*    TO 'media_service_user'@'%';

FLUSH PRIVILEGES;
