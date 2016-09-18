/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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

        $scope.deleteRole = function (role) {
            $http.delete("api/security/roles/" + role).then(function () {
                delete $scope.roles[role];
            });
        };

        $scope.createRole = function (role) {
            if (role) {
                $http.put("api/security/roles/" + role, []).then(function () {
                    $scope.roles[role] = [];
                });
            }
        };

        $scope.removePermission = function (role, permission) {
            var newPermissions = angular.copy($scope.roles[role]);
            for (var i = 0; i < newPermissions.length; i++) {
                var here = newPermissions[i];
                if (here.channel === permission.channel && here.permission == channel.permission) {
                    newPermissions.splice(i, 1);
                    break;
                }
            }
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

        $http.get('api/security/permissions').then(function (response) {
            $scope.permissions = response.data;
        });
        $http.get('api/security/roles').then(function (response) {
            $scope.roles = response.data;
            $http.get('api/security/users').then(function (response) {
                $scope.users = response.data.map(prepareUserForView);
            });
        });
    }
});