var phonecatApp = angular.module('phonecatApp', []);

phonecatApp.controller('PhoneListCtrl', ['$scope', '$http',
  function ($scope, $http) {
    $http.get('/js/study/angularjs/test1/data.js').success(function(data) {
      $scope.phones = data;
    });

    $scope.orderProp = 'age';
  }]);