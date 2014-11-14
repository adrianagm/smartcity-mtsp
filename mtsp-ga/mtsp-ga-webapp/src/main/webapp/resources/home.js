/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var mtsp = {
    stopList: [],
    map: null,
    routesGroup: null,
    initMap: function () {

        var map = L.map('map').setView([37.389254195966004, -5.977935791015625], 13);

        mtsp.routesGroup = L.featureGroup([]);
        mtsp.routesGroup.addTo(map);


        L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
            maxZoom: 18
        }).addTo(map);

        map.on("click", function (e) {
            mtsp.stopList.push({
                name: "Stop",
                latitude: e.latlng.lat,
                longitude: e.latlng.lng
            });

            L.marker([e.latlng.lat, e.latlng.lng]).addTo(map);

            $("#stopList").append("<li>" + e.latlng.lat + ", " + e.latlng.lng + "</li>");
        });

        mtsp.map = map;

    },
    calculateRoutes: function () {
        mtsp.routesGroup.clearLayers();
        $.ajax({
            method: "POST",
            url: "v1/calculateMTSP",
            data: JSON.stringify({origin: mtsp.stopList[0], stops: mtsp.stopList.slice(1)}),
            success: function (data) {
                console.debug(data);

                var colors = ["red","blue","green"];

                for (var i = 0; i < data.routes.length; i++) {
                    L.geoJson(data.routes[i].feature,{
                        style: function() {
                            return {color: colors[i]}
                        }
                    }).addTo(mtsp.routesGroup);
                }
            },
            dataType: "json",
            contentType: "application/json"
        });
    }
};


window.onload = mtsp.initMap;