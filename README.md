# Panda-Manta
System for multi-competiton turnaments

Used internally in LeanForge

## How to start and test project

### Create slack workspace
* Go to <https://slack.com/>
* Create new workspace
* Go to <https://api.slack.com/>
* Build new app
* Add `bots` feature and add bot user
* Install app to the workspace
* Copy `Bot User OAuth Access Token`
* On any channel in your workspace invite bot user

### Start application

#### Gradle
run command `SLACK_TOKEN="your_access_token" ./gradlew(.bat) run`

#### Runner
* set env variable SLACK_TOKEN with your workspace token
* run `com.leanforge.soccero.SocceroSpringApplication#main`

#### Local docker image
*  `./gradlew dockerBuildImage`
* `docker run -e SLACK_TOKEN=your_access_token mrdunski/soccero-panda-manta:SNAPSHOT`


## Code conventions

* Use `@org.springframework.stereotype.Repository` for data access
* Use `@org.springframework.stereotype.Service` for domain logic
* Use  separate service for code that works on 2 domains
* Use `@org.springframework.stereotype.Controller` to define entrypoints
* Test your stuff


## Domains

### Team

Team is group of players that plays together against other teams.

You can find there logic, that matches players together.

### League
Aggregates together players, teams and competitions
