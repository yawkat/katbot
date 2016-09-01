/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

katbotApp.component('karma', {
    templateUrl: "karma.template.html",
    controller: function KarmaController($scope, $http, $sanitize) {
        $scope.entries = [];
        $scope.search = "";
        $scope.page = 0;

        function update() {
            $http.get('api/karma?search=' + encodeURI($scope.search) + "&page=" + $scope.page).then(function (response) {
                if (response.data.length == 0 && $scope.page > 0) {
                    $scope.page = 0;
                    update();
                } else {
                    $scope.entries = response.data;
                }
            });
        }

        update();
        $scope.$watch("search", update);
        $scope.$watch("page", update);

        $scope.chartConfigBuilder = function (name) {
            return {
                title: {text: ''},
                options: {
                    chart: {zoomType: 'x'}
                },
                xAxis: {type: 'datetime'},
                yAxis: {title: {text: ''}},
                legend: {enabled: false},
                loading: true,
                func: function (plot) {
                    console.log("fetch");
                    $http.get("api/karma/" + name + "/history").then(function (response) {
                        var data = [];
                        var accumulated = 0;
                        for (var i = 0; i < response.data.length; i++) {
                            var entry = response.data[i];
                            accumulated += entry.delta;
                            data.push({
                                x: Date.parse(entry.timestamp),
                                y: accumulated,
                                actor: entry.actor,
                                delta: entry.delta
                            })
                        }
                        plot.addSeries({
                            type: 'area',
                            step: 'left',
                            showInLegend: false,
                            animation: false,
                            name: 'karma',
                            data: data,
                            color: '#55BF3B', // green
                            negativeColor: '#DF5353', // red
                            tooltip: {
                                pointFormatter: function () {
                                    var html = "";
                                    html += "Karma: <b>" + this.y + "</b>";

                                    var actor = this.actor;
                                    if (actor) {
                                        html += "<br>Actor: ";
                                        var hostIndex = actor.indexOf('@');
                                        if (hostIndex != -1) {
                                            var nick = $sanitize(actor.substring(0, hostIndex));
                                            var host = $sanitize(actor.substring(hostIndex));
                                            html += "<b>" + nick + "</b><span class='userHost'>" + host + "</span>";
                                        } else {
                                            html += "<b>" + $sanitize(actor) + "</b>";
                                        }
                                    }

                                    if (this.comment) {
                                        html += "<br>Comment: <b>" + $sanitize(this.comment) + "</b>"
                                    }

                                    html += "<br>Delta: <b>" + this.delta + "</b>";

                                    return html;
                                }
                            }
                        });
                        plot.hideLoading();
                    });
                }
            };
        };
    }
});