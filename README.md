# Abstract

A lot of people use their car to travel. But there are a lot of accidents in the roads, it's not safe.

A lot of people studied how to go between two points with the shortest path, but we tried a different approach. We think that the best way to travel is with a safe planning, so we decided to plan our route avoiding the most risky points in the roads.

# How it's build

It's an android app, that uses Google Maps to show a beautiful visualization of the region you are traveling. 
On top of that, we drawed our route planification computed with data of accidents provided by the Spain's government.

We build a back-end using Node.js as server, express.js as framework and elasticsearch as database to process the data we got from the internet into something we could use.

We also used the geolocalization from phone to send push notification through Yo when the user gets to the proximity of a risk point.

# Challenges

Trying to join Google Maps with our new route that avoids the risky spots.

# Competing prizes

## General prizes

## Zurich

With our hack, we tried to minimize the risks when you have to travel with your car.

## Yo

We used the simplest Yo API to get push notifications to alert our users when they are near some problematic and risky spot.
