/**
 * Created by j on 1/3/15.
 */
var Box = function (road,loc1,loc2) {


    this.road = road;
    this.box = {
        "top_left": {
            "lat": loc1.lat > loc2.lat? loc1.lat : loc2.lat,
            "lon": loc1.lng < loc2.lng? loc1.lng : loc2.lng
        },
        "bottom_right":{
            "lat": loc1.lat > loc2.lat? loc2.lat : loc1.lat,
            "lon": loc1.lng < loc2.lng? loc2.lng : loc1.lng
        }
    };

};

Box.prototype.getRoad = function(){
    return this.road;
};

Box.prototype.setRoad = function(road){
    this.road = road;
};

Box.prototype.getBox = function(){
    return this.box;
};

Box.prototype.setBox = function(box){
    this.box = box;
}

module.exports.Box = Box;