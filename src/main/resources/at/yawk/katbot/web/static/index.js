/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

var katbotApp = angular.module('katbotApp', []);

katbotApp.component('karma', {
    templateUrl: "karma.template.html",
    controller: function KarmaController($scope, $http) {
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
    }
});