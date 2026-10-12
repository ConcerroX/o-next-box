package com.mi.onextbox.notifications;

interface INotificationBatchController {
    String inspect(int userId, String targets);
    String apply(int userId, String mutations);
}
