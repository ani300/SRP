/**
 * Created by j on 1/3/15.
 */
var db = null;

var point = function (arg){
    this.lat = arg.lat? arg.lat: null;
    this.lng = arg.lng? arg.lng: null;
    this.route = arg.road? arg.road: null;
    this.km = arg.km? arg.km: null;
    this.rate = arg.rate? arg.rate: null;
    console.log("Point created");
    return this;
};

point.prototype.setLat = function(lat){
    this.lat = lat;
};

point.prototype.getLat = function(){
    return this.lat;
};


point.prototype.setLng = function(lng){
    this.lng = lng;
};

point.prototype.getLng = function(){
    return this.lng;
};

point.prototype.setRoad = function(road){
    this.road = road;
};

point.prototype.getRoad = function(){
    return this.road;
};

point.prototype.setKm = function(km){
    this.km = km;
};

point.prototype.getKm = function(){
    return this.km;
};

point.prototype.setRate = function(rate){
    this.rate = rate;
};

point.prototype.getRate = function(){
    return this.rate;
};

point.prototype.Save = function(){

    db.create({
        index:"srp-point",
        type:"point",
        body: this
    },function(err,resp){
        console.log(err);
        if(err) throw err;
    });
};

var searchPoint = function(box){
    var query={
        index:"srp-point",
        type:"point",
        body:{
            "query":{
                "filtered" : {
                    "query" : {
                        "field":{
                            "road" : box.getRoad()
                        }
                    },
                    "filter" : {
                        "geo_bounding_box" : {
                            "location" : box.getBox()
                        }
                    }
                }
            }
        }
    };

    db.search(query,function(err,resp){
        if(err) console.dir(err);
        else {
            if(res.hits.total != 0) return resp.hits.hits;
            else return [];
        }
    });
};


module.exports.BlackPoint = function(elastic){
    db = elastic;
    return {
        Point: function(arg){ return new point(arg);},
        Search: searchPoint
    };
};