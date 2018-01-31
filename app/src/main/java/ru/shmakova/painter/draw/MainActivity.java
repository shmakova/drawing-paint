package ru.shmakova.painter.draw;

import android.os.Bundle;
import android.support.annotation.Nullable;

import ru.shmakova.painter.R;
import ru.shmakova.painter.app.App;
import ru.shmakova.painter.screen.BaseActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(this).applicationComponent().inject(this);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_frame_layout, new DrawFragment())
                    .commit();
        }
    }
}
