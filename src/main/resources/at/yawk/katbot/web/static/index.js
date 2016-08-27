/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

var katbotApp = angular.module('katbotApp', ["ngCookies"]);

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

katbotApp.component('security', {
    templateUrl: "security.template.html",
    controller: function SecurityController($scope, $http) {
        $scope.permissions = [];
        $scope.roles = {};
        $scope.users = [];

        $scope.objectKeys = function (o) {
            return Object.keys(o).filter(function (e) {
                return o.hasOwnProperty(e)
            });
        };

        $scope.removePermission = function (role, permission) {
            var newPermissions = angular.copy($scope.roles[role]);
            newPermissions.splice(newPermissions.indexOf(permission), 1);
            $http.put('api/security/roles/' + role, newPermissions).then(function () {
                $scope.roles[role] = newPermissions;
            });
        };

        $scope.addPermission = function (role, permission) {
            var newPermissions = angular.copy($scope.roles[role]);
            newPermissions.push(permission);
            $http.put('api/security/roles/' + role, newPermissions).then(function () {
                $scope.roles[role] = newPermissions;
            });
        };

        function prepareUserForView(user) {
            user = angular.copy(user);
            user.$$roles = {};
            for (var role in $scope.roles) {
                if ($scope.roles.hasOwnProperty(role)) {
                    user.$$roles[role] = function () {
                        var finalRole = role;
                        return function (newHasRole) {
                            var oldIndex = user.roles.indexOf(finalRole);
                            if (arguments.length) { // set
                                var newRoles = angular.copy(user.roles);
                                if (oldIndex != -1) {
                                    newRoles.splice(oldIndex, 1);
                                }
                                if (newHasRole) {
                                    newRoles.push(finalRole);
                                }

                                $http.put('api/security/users/' + user.username + '@' + user.host, newRoles).then(function () {
                                    user.roles = newRoles;
                                });
                            } else { // get
                                return oldIndex != -1;
                            }
                        };
                    }();
                }
            }
            return user;
        }

        $scope.addUser = function (nick, host) {
            if (nick && host) {
                $scope.users.push(prepareUserForView({username: nick, host: host, roles: []}));
            }
        };

        /*
         $http.get('api/security/permissions').then(function (response) {
         $scope.permissions = response.data;
         });
         */
        $http.get('api/security/roles').then(function (response) {
            $scope.roles = response.data;
            $http.get('api/security/users').then(function (response) {
                $scope.users = response.data.map(prepareUserForView);
            });
        });
    }
});