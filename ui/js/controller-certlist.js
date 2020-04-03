function certListReload($scope, $http, $rootScope, onOk) {
    httpGet($rootScope, $http, '/openvpn/allcert', function(response1) {
        tableCreateOrUpdate('#general-table', response1.data.data, [
            { "data": "fileName" },
            { "data": "validFrom" },
            { "data": "validTo" },
            { "data": "subject" },
            { "data": "fileName",
                render: function(data, type, full) {
                    return "<a href='/openvpn/certdl?type=ovpn&file="+encodeURI(data)+"'><u>ovpn</u></a>&nbsp;&nbsp;" +
                        "<a href='/openvpn/certdl?type=zip&file="+encodeURI(data)+"'><u>zip</u></a>";
                }
            }
        ]);
        onOk();
    });
}

app.controller("certlist", function ($scope, $http, $rootScope) {
    certListReload($scope, $http, $rootScope, function() {
        $rootScope.$broadcast('setupHeader', {
            globalHeader: 'Certificates list',
            headerLinks: [
                { name: 'Add certificate', url: '#!certcreate', onclick: null},
                { name: 'Reload data', url: null, onclick: function() {
                    certListReload($scope, $http, $rootScope, function() {
                        $rootScope.$broadcast('alertMessage', { type: 'success', msg: "Data reloaded!" });
                    });
                }}
            ]
        });
    });
});