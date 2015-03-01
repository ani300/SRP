var express = require('express');
var router = express.Router();
var es = require("elasticsearch");
var db = new es.Client({
    host: "127.0.0.1:9200",
    log:["trace"]
});
var BlackPoint = require('./../model/BlackPoint.js').BlackPoint(db);
var Box = require('./../model/Box.js').Box;
var async = require("async");

/* GET home page. */
router.get('/', function(req, res) {
  res.render('index', { title: 'Express' });
});



router.post('/point/add', function(req,res){
    console.log(req.body);
    var pt = BlackPoint.Point(req.body);
    console.log(JSON.stringify(pt));
    console.dir(pt);
    pt.Save();
    res.redirect('/');
});

/**
 Query input format:
 {
 "signs": [ ],
"index": 5,
"maneuverNotes": [ ],
"direction": 4,
"narrative": "Turn slight right onto C-55/Ronda Exterior Est de Manresa.",
"iconUrl": "http://content.mapquest.com/mqsite/turnsigns/rs_slight_right_sm.gif",
"distance": 0.329,
"time": 41,
"linkIds": [ ],
"streets": [
    "C-55",
    "Ronda Exterior Est de Manresa"
],
"attributes": 0,
"transportMode": "AUTO",
"formattedTime": "00:00:41",
"directionName": "South",
"mapUrl": "http://open.mapquestapi.com/staticmap/v4/getmap?key=Fmjtd|luu8210ynq,8w=o5-94r504&type=map&size=225,160&pois=purple-6,41.734397,1.850839,0,0|purple-7,41.729789,1.851537,0,0|&center=41.732093,1.851188&zoom=11&rand=1302577460&session=54f264d3-0352-0006-02b7-0797-00163e2ca77d",
"startPoint": {
    "lng": 1.850839,
    "lat": 41.734397
},
"turnType": 1
},


 [{"streets": [
    "C-55",
],
"startPoint": {
    "lng": 0,
    "lat": 0
}
},
 {"streets": [
    "C-55",
],
"startPoint": {
    "lng": 2,
    "lat": 2
}
}]
 *
 */

router.post('/point/route',function(req,res){


    var maneuvers =req.param("maneuvers",null);


    var array = maneuvers.map( function (elem, index) {
        var box = null;
        if(index != maneuvers.length-1){
            if(elem.streets && elem.streets.length!= 0) {
                box = new Box(elem.streets[0], elem.startPoint, maneuvers[index+1].startPoint);
                return function (cb){
                    console.log("In kek func");
                    console.dir(box);
                    if(box) BlackPoint.Search(box,cb);
                    else cb(null, []);
                };
            }
        } else {
            return function(cb){cb(null,[]);};
        }
    });

    async.parallel(array, function(err,results) {

        console.log("In res func");

        if(err) console.dir(err);
        else res.json({blackPoints:results});
    });

});
module.exports = router;
