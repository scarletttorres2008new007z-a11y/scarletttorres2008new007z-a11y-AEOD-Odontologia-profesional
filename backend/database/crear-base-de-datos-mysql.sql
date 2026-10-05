-- MySQL / MariaDB (XAMPP). Ejecutar una sola vez (phpMyAdmin o la consola de base de datos de IntelliJ).
-- utf8mb4 permite guardar tildes, ñ y cualquier carácter. Las tablas las crea la aplicación al arrancar.
CREATE DATABASE IF NOT EXISTS clinica_landing
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
