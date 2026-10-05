-- SQL Server. Ejecutar una sola vez (SSMS o la consola de base de datos de IntelliJ).
-- Las tablas las crea la aplicación al arrancar.
IF DB_ID(N'clinica_landing') IS NULL
    CREATE DATABASE clinica_landing;
GO
