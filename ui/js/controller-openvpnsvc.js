app.controller("openvpnsvc", function ($scope, $http, $rootScope) {
    $rootScope.$broadcast('setupHeader', {
        globalHeader: 'OpenVPN service',
        headerLinks: [
            { name: 'Stop service', url: null, onclick: function() {
                httpPost($rootScope, $http, '/openvpn/stop', { data: 'dummy' }, function(response1) {
                    if (response1.data.data === "OK") {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Service stopped" });
                    } else {
                        $rootScope.$broadcast('alertMessage', { type: 'danger', msg: "Service not running"});
                    }
                    httpGet($rootScope, $http, '/openvpn/log64k', function(response1) {
                        $scope.openVpnLogs = response1.data.data;
                    });
                });
            }},
            { name: 'Start service', url: null, onclick: function() {
                httpPost($rootScope, $http, '/openvpn/run', { data: 'dummy' }, function(response1) {
                    if (response1.data.data === "OK") {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Service started" });
                    } else {
                        $rootScope.$broadcast('alertMessage', { type: 'danger', msg: "Service already started"});
                    }
                    httpGet($rootScope, $http, '/openvpn/log64k', function(response1) {
                        $scope.openVpnLogs = response1.data.data;
                    });
                });
            }}
        ]
    });

    $scope.openVpnState = "OpenVPN state unknown";

    httpGet($rootScope, $http, '/openvpn/version', function(response1) {
        $scope.openVpnVersion = response1.data.data;
    });

    httpGet($rootScope, $http, '/openvpn/log64k', function(response1) {
        $scope.openVpnLogs = response1.data.data;
    });

    $scope.$on('svcstate', function(event, obj){
        $scope.openVpnState = (obj.isRunning === true) ? "OpenVPN is running" : "OpenVPN is stopped";
        $scope.$apply();
    });

    waitForEx(function() {
        return stompClient.usr_subscribed === true;
    }, function() {
        stompClient.send("/app/general", {}, "{\"cmd\":\"IS_OVPN_RUN\", \"data\":{}}");
    });
});
