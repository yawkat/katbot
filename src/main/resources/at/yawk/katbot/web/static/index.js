/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

var katbotApp = angular.module('katbotApp', ["ngCookies", "highcharts-ng", "ngSanitize"]);

katbotApp.run(function ($http, $cookies) {
    if (window.location.hash.indexOf("#auth:") == 0) {
        $cookies.put("KATBOT_AUTH_TOKEN", window.location.hash.substr("#auth:".length));
        // clear hash
        history.pushState('', document.title, window.location.pathname);
    }
    var authTokenFromCookie = $cookies.get("KATBOT_AUTH_TOKEN");
    $http.defaults.headers.common.Authorization = 'Token ' + authTokenFromCookie;
});

katbotApp.controller('RootController', function ($scope) {
    $scope.KARMA = "karma";
    $scope.SECURITY = "security";

    $scope.section = $scope.KARMA;

    var sections = [$scope.SECURITY, $scope.KARMA];
    var hash = window.location.hash;
    if (hash.length > 0) {
        hash = hash.substr(1);
        if (sections.indexOf(hash) != -1) {
            $scope.section = hash;
        }
    }

    $scope.$watch("section", function () {
        window.location.hash = "#" + $scope.section;
    });
});