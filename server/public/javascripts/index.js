/**
 * Created by j on 1/3/15.
 */


function initialize() {
    var mapProp = {
        center:new google.maps.LatLng(41.7803788,1.8102622),
        zoom:7,
        mapTypeId:google.maps.MapTypeId.ROADMAP
    };
    var map=new google.maps.Map(document.getElementById("googleMap"),mapProp);

    google.maps.event.addListener(map, 'click', function(event) {
        placeMarker(event.latLng);
    });

    function placeMarker(location) {
        console.dir(location);
        document.getElementById('lat').value = location.k;
        document.getElementById('lng').value = location.D;
        var marker = new google.maps.Marker({
            position: location,
            map: map
        });
    }
}

google.maps.event.addDomListener(window, 'load', initialize);
