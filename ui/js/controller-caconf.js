function caconfReload($scope, $http, $rootScope, onOk) {
    httpGet($rootScope, $http, '/openvpn/settings', function(response) {
        $scope.s_issuer_cn = response.data.data.s_issuer_cn;
        $scope.s_issuer_ou = response.data.data.s_issuer_ou;
        $scope.s_issuer_o = response.data.data.s_issuer_o;
        $scope.s_issuer_l = response.data.data.s_issuer_l;
        $scope.s_issuer_s = response.data.data.s_issuer_s;
        $scope.s_issuer_c = response.data.data.s_issuer_c;
        $scope.s_issuer_email = response.data.data.s_issuer_email;
        onOk();
    });
}

app.controller("caconf", function ($scope, $http, $rootScope) {
    caconfReload($scope, $http, $rootScope, function() {
        $rootScope.$broadcast('setupHeader', {
            globalHeader: 'CA configuration',
            headerLinks: [
                { name: 'Save changes', url: null, onclick: function() {
                    httpPost($rootScope, $http, '/openvpn/settings', {
                        s_issuer_cn: $scope.s_issuer_cn,
                        s_issuer_ou: $scope.s_issuer_ou,
                        s_issuer_o: $scope.s_issuer_o,
                        s_issuer_l: $scope.s_issuer_l,
                        s_issuer_s: $scope.s_issuer_s,
                        s_issuer_c: $scope.s_issuer_c,
                        s_issuer_email: $scope.s_issuer_email
                    }, function(response) {
                        caconfReload($scope, $http, $rootScope, function() {
                            $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Configuration saved!" });
                        });
                    })
                }},
                { name: 'Forget & reload', url: null, onclick: function() {
                    caconfReload($scope, $http, $rootScope, function() {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Configuration reloaded!" });
                    });
                }}
            ]
        });
    });
});
