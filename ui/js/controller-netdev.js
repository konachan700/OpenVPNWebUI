function netdevRenderer(data, type, full) {
    return data.replace(/\B(?=(\d{3})+(?!\d))/g, " ")
}

function netdevReload($scope, $http, $rootScope, onOk) {
    httpGet($rootScope, $http, '/netstat/netdev', function(response1) {
        tableCreateOrUpdate('#general-table', response1.data, [
           { "data": "iface" },
           { "data": "inBytes", render: netdevRenderer },
           { "data": "inPackets", render: netdevRenderer },
           { "data": "inErrors", render: netdevRenderer },
           { "data": "outBytes", render: netdevRenderer },
           { "data": "outPackets", render: netdevRenderer },
           { "data": "outErrors", render: netdevRenderer }
        ]);
        onOk();
    });
}

app.controller("netdev", function ($scope, $http, $rootScope) {
    netdevReload($scope, $http, $rootScope, function() {
        $rootScope.$broadcast('setupHeader', {
            globalHeader: 'Network statistics',
            headerLinks: [
                { name: 'Reload data', url: null, onclick: function() {
                    netdevReload($scope, $http, $rootScope, function() {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Data reloaded!" });
                    });
                }}
            ]
        });
    });
});