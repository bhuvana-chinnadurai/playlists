create table IF NOT EXISTS PLAYLISTS ( ID uuid not null primary key default uuid_generate_v4(),TITLE varchar(100) not null unique,
    DESCRIPTION text
);

