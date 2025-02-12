/*
 * Copyright (C) 2020 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stream_suite.link.shared.view;

import android.app.Activity;

import org.junit.Test;
import org.robolectric.Robolectric;

import com.stream_suite.link.MessengerRobolectricSuite;
import com.stream_suite.link.view.ColorPreviewButton;

import static org.junit.Assert.*;

public class ColorPreviewButtonTest extends MessengerRobolectricSuite {

    @Test
    public void test_create() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        ColorPreviewButton button = new ColorPreviewButton(activity);
        activity.setContentView(button);

        assertNotNull(button);
    }

}