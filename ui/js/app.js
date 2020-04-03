var app = angular.module("openvpn", ["ngRoute", 'luegg.directives']);
app.config(function($routeProvider) {
    $routeProvider
    .when("/", {
        templateUrl : "tpl/openvpn-svc.html",
        controller: 'openvpnsvc'
    })
    .when("/opened-ports", {
        templateUrl : "tpl/opened-ports.html",
        controller: "opened-ports"
    })
    .when("/configs", {
        templateUrl : "tpl/openvpn-configs.html",
        controller : "configs"
    })
    .when("/caconf", {
        templateUrl : "tpl/openvpn-caconf.html",
        controller : "caconf"
    })
    .when("/certdefaults", {
        templateUrl : "tpl/openvpn-certdefaults.html",
        controller : "certdefaults",
        createCert : false
    })
    .when("/certcreate", {
        templateUrl : "tpl/openvpn-certdefaults.html",
        controller : "certdefaults",
        createCert : true
    })
    .when("/certlist", {
        templateUrl : "tpl/openvpn-certlist.html",
        controller : "certlist"
    })
    .when("/netdev", {
        templateUrl : "tpl/netdev.html",
        controller : 'netdev'
    })
//    .when("/cert/settings", {
//        templateUrl : "tpl/openvpn-cert-settings.html"
//    });
});

var stompClient = null;
var socketClient = null;

app.controller('pageRoot', function($scope, $http, $rootScope) {
    $scope.isDocReady = 0;
    $http({
        method : "GET",
        url : '/openvpn/ready',
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
        data: ''
    }).then(function mySuccess(response) {
        if (response.data.data == "YES") {
            $scope.isDocReady = 2;
        } else {
            $scope.isDocReady = 1;
        }
    }, function myError(response) {
        $scope.isDocReady = 0;
    });

    socketClient = new SockJS('/websocket');
    stompClient = Stomp.over(socketClient);
    stompClient.connect({}, function(frame) {
        //console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/general', function(messageOutput) {
            var json = JSON.parse(messageOutput.body);
            switch (json.cmd) {
                case 'LOGS':

                    break;
                case 'IS_OVPN_RUN':
                    $rootScope.$broadcast('svcstate', {
                        isRunning: json.data
                    });
                    break;
            }
        });
        stompClient.usr_subscribed = true;
    });
});

app.controller('headerController', function($scope) {
    $scope.$on('setupHeader', function(event, obj){
        $scope.globalHeader = obj.globalHeader;
        $scope.headerLinks = obj.headerLinks;
    });
});

app.controller('msgController', function($scope, $timeout) {
    $scope.$on('alertMessage', function(event, obj){
       $scope.popupVisible = true;
       $scope.popupMessage = obj.msg;
       $scope.popupType = obj.type;
       $timeout(function() {
         $scope.popupVisible = false;
       }, 2500);
    });
});
