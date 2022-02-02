


# Playlists
This is a micro service , written in scala for creating/displaying playlists with the following frameworks/libraries and databases respectively.
- `http4s` to create micro service https://http4s.org/v0.20/service/ 
- `fly-way` to run db migrations. https://flywaydb.org/
- `quill` to write scala code to interact with postgres. https://getquill.io/#
- `postgres` as a database using docker https://hub.docker.com/_/postgres


##Dependencies
- [sbt ](https://www.scala-sbt.org/) 1.5.x 
- [Docker](Download Docker from: https://www.docker.com/) 

## How to run the service

Run the service `playlists` by executing  

`docker-compose up --force-recreate` 

This will start the service running at `localhost:8080` 

## How to test the service

Test the service by executing

`docker-compose up --force-recreate -d postgres`

`sbt test`

## Playlists

## Available Endpoints
The end points are:

Method | Url         | Description
------ | ----------- | -----------
POST   | /playlists      | Creates a playlist, with the given the title,description and author. Returns a 201 as a response when it succeeds .Returns 400 if the title already exists. otherwise returns 500.
GET    | /playlists/{id} | Returns the playlist with 200 if exists or 404 if not exists for the given id ,403 for the invalid id
PUT    | /playlists/{id} | Updates the playlist with 200 if exists or 404 if not exists for the given id ,403 for the invalid id
DELETE | /playlists/{id} | Deletes the playlist with 200 if exists or 404 if not exists for the given id ,403 for the invalid id

## Curl requests

### Create a new playlist
To create a new playlist

```
curl --location --request POST 'http://localhost:8080/playlists' \
--header 'Content-Type: application/json' \
--data-raw '{"title": "new playlist","description": "my playlist"}'
```

### Get a specific playlist by id
To list all the playlists

```
curl --location --request GET 'http://localhost:8080/playlists/6c359009-ed1e-4ea3-a887-5aa94c8ff70c'
```

### Update the playlist by id
To update a particular playlist

```
curl --location --request PUT 'http://localhost:8080/playlists/4d91bfdd-4dc6-460e-a3f3-5bddaef38d86' \
--header 'Content-Type: application/json' \
--data-raw '{"description": "my new playlists", "title": "playlist 3"}'
```


### Delete a playlist by id
To delete a particular playlist

```
 curl --location --request DELETE 'http://localhost:8080/playlists/431178e2-d3e1-42a8-ad78-b48504b7cfbe'
```