# Playlists
This is a micro service , written in scala for creating/displaying playlists with the following frameworks/libraries and databases respectively.
- `http4s` to create micro service https://http4s.org/v0.20/service/ 
- `fly-way` to run db migrations. https://flywaydb.org/
- `quill` to write scala code to interact with postgres. https://getquill.io/#
- `postgres` as a database using docker https://hub.docker.com/_/postgres

# How to run

sbt run

# Installation

docker run -d --name postgres-server -p 5432:5432 -e "POSTGRES_PASSWORD=password" postgres
docker exec -it 149b6545485e /bin/sh

# Playlists

## Available Endpoints
The end points are:

Method | Url         | Description
------ | ----------- | -----------
POST   | /playlists      | Creates a playlist, with the given the title,description and author, and returns a 201 as a response.
GET    | /playlists/{id} | Returns the playlists or 404 for the given id ,403 for the invalid id

## Curl requests

## Create a new playlist
To create a new playlist

```
curl --location --request POST 'http://localhost:8080/playlists' \
--header 'Content-Type: application/json' \
--data-raw '{"title": "new playlist","description": "my playlist"}'
```

## Get a specific playlist by id
To list all the playlists

```
curl --location --request GET 'http://localhost:8080/playlists/6c359009-ed1e-4ea3-a887-5aa94c8ff70c'
```
