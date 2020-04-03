function configsReload($scope, $http, $rootScope, onOk) {
    httpGet($rootScope, $http, '/openvpn/config', function(response) {
        $scope.serverTA = atob(response.data.data.server);
        $scope.clientTA = atob(response.data.data.client);
        onOk();
    })
}

app.controller("configs", function ($scope, $http, $rootScope) {
    configsReload($scope, $http, $rootScope, function() {
        $rootScope.$broadcast('setupHeader', {
            globalHeader: 'Openvpn configuration',
            headerLinks: [
                { name: 'Save changes', url: null, onclick: function() {
                    httpPost($rootScope, $http, '/openvpn/config', {
                        server: btoa($scope.serverTA),
                        client: btoa($scope.clientTA)
                    }, function(response) {
                        configsReload($scope, $http, $rootScope, function() {
                            $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Configuration saved!" });
                        });
                    })
                }},
                { name: 'Forget & reload', url: null, onclick: function() {
                    configsReload($scope, $http, $rootScope, function() {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Configuration reloaded!" });
                    });
                }}
            ]
        });
    });
});
