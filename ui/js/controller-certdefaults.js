function certdefaultsReload($scope, $http, $rootScope, onOk) {
    httpGet($rootScope, $http, '/openvpn/settings', function(response) {
        $scope.s_subject_cn = response.data.data.s_subject_cn;
        $scope.s_subject_ou = response.data.data.s_subject_ou;
        $scope.s_subject_o = response.data.data.s_subject_o;
        $scope.s_subject_l = response.data.data.s_subject_l;
        $scope.s_subject_s = response.data.data.s_subject_s;
        $scope.s_subject_c = response.data.data.s_subject_c;
        $scope.s_subject_email = response.data.data.s_subject_email;
        onOk();
    });
}

app.controller("certdefaults", function ($scope, $http, $rootScope, $route, $location) {
    let isCreate = $route.current.$$route.createCert;
    let txtHeader = (isCreate) ? 'Create certificate' : 'Certificate defaults';
    let txtOkLink = (isCreate) ? 'Create certificate' : 'Save changes';

    certdefaultsReload($scope, $http, $rootScope, function() {
        $rootScope.$broadcast('setupHeader', {
            globalHeader: txtHeader,
            headerLinks: [
                { name: txtOkLink, url: null, onclick: function() {
                    httpPost($rootScope, $http, '/openvpn/settings', {
                        s_subject_cn: $scope.s_subject_cn,
                        s_subject_ou: $scope.s_subject_ou,
                        s_subject_o: $scope.s_subject_o,
                        s_subject_l: $scope.s_subject_l,
                        s_subject_s: $scope.s_subject_s,
                        s_subject_c: $scope.s_subject_c,
                        s_subject_email: $scope.s_subject_email
                    }, function(response) {
                        if (isCreate) {
                            httpPost($rootScope, $http, '/openvpn/createcert', {
                                "name": $scope.s_subject_cn.replace(/[^A-Z0-9]+/ig, "_")
                            }, function(response) {
                                certdefaultsReload($scope, $http, $rootScope, function() {
                                    $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Certificate created!" });
                                    $location.path("/certlist");
                                });
                            });
                        } else {
                            certdefaultsReload($scope, $http, $rootScope, function() {
                                $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Configuration saved!" });
                            });
                        }
                    })
                }},
                { name: 'Forget & reload', url: null, onclick: function() {
                    certdefaultsReload($scope, $http, $rootScope, function() {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Configuration reloaded!" });
                    });
                }}
            ]
        });
    });
});
