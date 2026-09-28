package com.banking.ui;

import java.util.function.Consumer;

interface NovaBankNavigable {
    void setNavigator(Consumer<String> navigator);
}
