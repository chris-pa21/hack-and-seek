-- Dadurch, dass JPA alle Tabellen und Relationen erstellt,
-- ist es nicht nötig, diese in diesem create.sql zu erstellen.

-- Lösche und erstelle die Datenbank neu
DROP DATABASE IF EXISTS joust;
CREATE DATABASE joust;

-- Wechsle zur neuen Datenbank
\c joust

-- Erstelle den Benutzer und die Rolle
CREATE USER joustadmin WITH PASSWORD 'testtest';

-- Gewähre die Rechte nach der Erstellung der Tabellen
GRANT ALL PRIVILEGES ON DATABASE joust TO joustadmin;

-- Rechte auf das public-Schmema geben
GRANT CREATE ON SCHEMA public TO joustadmin;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO joustadmin;

-- Rechte auf Tabellen
GRANT SELECT, UPDATE, REFERENCES ON admin TO joustadmin;
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON team TO joustadmin;
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON arena TO joustadmin;
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tournament TO joustadmin;
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tournament_participation TO joustadmin;
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON scored_match TO joustadmin;
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON elimination_match TO joustadmin;
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON placement TO joustadmin;


-- WAS MUSS ICH MACHEN, DASS ALLES LÄUFT?
-- 1. Diese Datei aus der Wissensdatenbank holen
-- 2. in der CMD folgender command: "docker cp /pfad/zu/diesem/create.sql CONTAINERNAME:/pfad/wo/die/datei/sein/soll"
-- 3.in der Docker Konsole die Postgreskonsole starten mit: psql -U postgres
-- 4. Script ausführen mit: \i /pfad/zu/create.sql