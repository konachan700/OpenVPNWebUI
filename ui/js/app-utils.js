function waitForEx(expr, callback) {
    if (expr() == true) {
        callback();
    } else {
        setTimeout(function() {
            waitForEx(expr, callback);
        }, 50);
    }
}

function tableCreateOrUpdate(id, data, columns) {
    if ($.fn.dataTable.isDataTable(id)) {
        let datatable = $(id).DataTable();
        datatable.clear();
        datatable.rows.add(data);
        datatable.draw();
    } else {
        $(id).DataTable({
            "data" : data,
            "columns": columns
        });
    }
}

function httpPost($rootScope, $http, url, data, onOk) {
    $http({
        method : "POST",
        url : url,
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
        data : data
    }).then(function mySuccess(response) {
        onOk(response);
    }, function myError(response) {
        $rootScope.$broadcast('alertMessage', { type: 'danger', msg: "Communication error; " + response.statusText});
    });
}

function httpGet($rootScope, $http, url, onOk) {
    $http({
        method : "GET",
        url : url,
        headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
        data: ''
    }).then(function mySuccess(response) {
        onOk(response);
    }, function myError(response) {
         $rootScope.$broadcast('alertMessage', { type: 'danger', msg: "Communication error; " + response.statusText});
         $rootScope.$broadcast('setupHeader', {
             globalHeader: 'Error',
             headerLinks: []
         });
    });
}
