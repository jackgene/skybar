function onCoverageUpdate($scope, $timeout) {
  function resetJustUpdated(sourceLine) {
    $timeout(function () {
      sourceLine.justUpdated = false;
    }, 50);
  }
  if (typeof $scope.currentSourceFile === "string") {
    var currentSourceFileCoverage = $scope.coverage[$scope.currentSourceFile];
    for (var i = 0; i < $scope.currentSourceLines.length; i++) {
      var sourceLine = $scope.currentSourceLines[i];
      var execCount = currentSourceFileCoverage[sourceLine.number];
      if (typeof execCount === "number") {
        console.log("updating coverage for line " + sourceLine.number);
        sourceLine.executable = true;
        sourceLine.covered = execCount > 0;
        sourceLine.justUpdated = (sourceLine.execCount != execCount);
        if (sourceLine.justUpdated) {
          resetJustUpdated(sourceLine);
        }
        sourceLine.execCount = execCount;
      } else {
        sourceLine.executable = false;
      }
    }
  }
};

function openWebSocket($scope, $timeout) {
  var host = location.host;
  var wsUri = "ws://" + host + "/livecoverage/";
  var websocket = new WebSocket(wsUri);

  websocket.onopen = function (evt) {
    console.log("onOpen Event")
  };
  websocket.onclose = function (evt) {
    console.log("onClose Event")
  };
  websocket.onmessage = function (evt) {
    var parsed = JSON.parse(evt.data);
    if(typeof $scope.coverage === "undefined") {
      console.log("got initial coverage: "+evt.data)
      $scope.coverage = parsed
    }
    else{
      console.log("got incremental update: "+evt.data)
      for(var sourceFile in parsed)
      {
         var newFileLines = parsed[sourceFile]
         var oldFileLines = $scope.coverage[sourceFile] || { sourceFile: {}}
         // make sure the object is stored in scope
         $scope.coverage[sourceFile] = oldFileLines
         for(var lineNum in newFileLines)
         {
            var increment = newFileLines[lineNum]
            var oldExecCount = oldFileLines[lineNum] || 0

            oldFileLines[lineNum] = oldExecCount + increment

         }
      }
    }
    $scope.$apply(function() {
        onCoverageUpdate($scope, $timeout);
    });

  };
  websocket.onerror = function (evt) {
    console.log("onError Event")
  };

};

angular.module('skybar', ['treeControl'])
    .controller('SkybarController', ['$scope', '$interval', '$http', '$timeout',
    function ($scope, $interval, $http, $timeout) {

    openWebSocket($scope, $timeout)

    $scope.sourceFiles = function () {
        var sourceFiles = [];
        for (var sourceFile in $scope.coverage) {
            sourceFiles.push(sourceFile);
        }
        return sourceFiles;
    };

    $scope.sourceTreeOptions = {
      "dirSelectable": false
    }
    var sourceTreeRoot = {"children": []};
    $scope.sourceTree = function (coverage) {
        function pushIfNewAndGet(name, isFile, packages) {
            for (var i = 0; i < packages.length; i ++) {
                if (packages[i].name === name) return packages[i];
            }
            var newPackage = {"name": name};
            if (!isFile) {
                newPackage.children = [];
            }
            packages.push(newPackage);
            return newPackage;
        }
        for (var sourceFile in coverage) {
            var currentTraversalNode = sourceTreeRoot;
            var subPackages = sourceFile.substring(0, sourceFile.lastIndexOf("/")).split("/");
            for (var i = 0; i < subPackages.length; i ++) {
                currentTraversalNode = pushIfNewAndGet(subPackages[i], false, currentTraversalNode.children);
            }

            var sourceFileName = sourceFile.substring(sourceFile.lastIndexOf("/") + 1);
            var fileNode = pushIfNewAndGet(sourceFileName, true, currentTraversalNode.children);
            fileNode.sourceFile = sourceFile;
        }

        return sourceTreeRoot;
    }

    $scope.loadSource = function (sourceFile) {
        console.log("sourceFile = " + sourceFile)
        $http.get(
            '/source/' + sourceFile
        ).success(function (data) {

              var sourceLineTexts = data.split("\n")
              $scope.currentSourceLines = []

              for (var i = 0; i < sourceLineTexts.length; i++) {
                  $scope.currentSourceLines.push(
                    { "text": sourceLineTexts[i], "number": (i + 1).toString() }
                  )
              }
              $scope.currentSourceFile = sourceFile;
              onCoverageUpdate($scope, $timeout);

              console.log($scope.currentSourceLines)
          }).error(function (data, status) {
              console.log("error loading coverage data:");
              console.log("status: " + status);
              console.log("data: " + data);
          })
    };

    $scope.getExecCount = function (coverage, sourceFile, lineNumber) {
        var sourceFileCoverage = coverage[sourceFile];
        return sourceFileCoverage[lineNumber.toString()];
    };

}]);