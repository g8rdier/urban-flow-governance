package ufg.app

class ApiResponse {
    String status
    String msg
    Map data = [:]

    static ApiResponse success(String msg = '') {
        new ApiResponse(status: 'success', msg: msg ?: '')
    }

    static ApiResponse success(String msg, Map data) {
        new ApiResponse(status: 'success', msg: msg ?: '', data: data ?: [:])
    }

    static ApiResponse failure(String msg) {
        new ApiResponse(status: 'failure', msg: msg ?: '')
    }
}