function openedPortsReload($scope, $http, $rootScope, onOk) {
    httpGet($rootScope, $http, '/netstat/tupln?name=tcp', function(response1) {
        tableCreateOrUpdate('#general-table-tcp', response1.data.data, [
           { "data": "port" },
           { "data": "ip" },
           { "data": "protocol" }
        ]);
        httpGet($rootScope, $http, '/netstat/tupln?name=udp', function(response2) {
            tableCreateOrUpdate('#general-table-udp', response2.data.data, [
               { "data": "port" },
               { "data": "ip" },
               { "data": "protocol" }
            ]);
            onOk();
        });
    });
}

app.controller("opened-ports", function ($scope, $http, $rootScope) {
    openedPortsReload($scope, $http, $rootScope, function() {
        $rootScope.$broadcast('setupHeader', {
            globalHeader: 'Opened ports',
            headerLinks: [
                { name: 'Reload data', url: null, onclick: function() {
                    openedPortsReload($scope, $http, $rootScope, function() {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Data reloaded!" });
                    });
                }}
            ]
        });
    });
});