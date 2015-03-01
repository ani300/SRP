var express = require('express');
var router = express.Router();
var es = require("elasticsearch");
var db = new es.Client({
    host: "127.0.0.1:9200",
    logs: ["trace","error"]
});
var BlackPoint = require('./../model/BlackPoint.js').BlackPoint(db);
var Box = require('./../model/Box.js').Box;
var async = require("async");

/* GET home page. */
router.get('/', function(req, res) {
  db.search({
      index:"srp-point",
      type: "point",
      body:{
          query : {
              match_all : {}
          }
      }
  },function(err,resp){
      if(!err){
          res.render('index', { title: 'Safe Route Planner', points: resp.hits.total > 0? resp.hits.hits.map(function(elem){return elem._source}):null });
      } else throw err;
  });

});



router.post('/point/add', function(req,res){
    console.log(req.body);
    var pt = BlackPoint.Point(req.body);
    console.log(JSON.stringify(pt));
    console.dir(pt);
    pt.Save();
    res.redirect('/');
});

router.post('/point/route',function(req,res){

    sendYo();

    var maneuvers =req.param("maneuvers",null);



    var array = maneuvers.map( function (elem, index) {
        var box = null;
        if(index != maneuvers.length-1){
            if(elem.streets && elem.streets.length!= 0) {
                box = new Box(elem.streets[0], elem.startPoint, maneuvers[index+1].startPoint);
                return function (cb){
                    if(box) BlackPoint.Search(box,cb);
                    else cb(null, []);
                };
            }
            return function(cb){cb(null,[]);};
        } else {
            return function(cb){cb(null,[]);};
        }
    });

    async.parallel(array, function(err,results) {

        console.log("In res func");

        if(err) console.dir(err);
        else {
            var arr = [];
            for(var i = 0; i < results.length; ++i) arr = arr.concat(results[i]);
            res.json({blackPoints:arr});
        }
    });

});

router.get('/yo',function(req,res){
    sendYo();
    res.sendStatus(200);
});

module.exports = router;



function sendYo(){
    var request = require('request');

    request.post(
        'http://api.justyo.co/yoall/',
        { form: { 'api_token': 'b0116fee-8f13-464c-84d7-6908d02bade7'} },
        function (error, response, body) {
            if (!error && response.statusCode == 200) {
                console.log(body);
            }
        }
    );
}
