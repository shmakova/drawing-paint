package ru.shmakova.painter.presentation.draw;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.IOException;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import ru.shmakova.painter.R;
import ru.shmakova.painter.presentation.draw.brush.BrushPickerDialogFragment;
import ru.shmakova.painter.presentation.draw.text.TextDialogFragment;
import ru.shmakova.painter.utils.ImageUtils;
import rx.Observable;
import rx.subjects.PublishSubject;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

@SuppressWarnings("PMD.GodClass") // more than 47 methods
public class DrawFragment extends Fragment implements
        TextDialogFragment.EditTextDialogListener,
        BrushPickerDialogFragment.BrushPickerDialogListener,
        DrawView {
    private static final int GALLERY_PICTURE_REQUEST_CODE = 10;
    private static final int SAVE_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 20;
    private static final int SHARE_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 30;
    private static final int TEXT_PICKER_REQUEST_CODE = 40;
    private static final int BRUSH_PICKER_REQUEST_CODE = 60;

    private DataFragment dataFragment;

    @NonNull
    private final PublishSubject<Integer> colorPicks = PublishSubject.create();

    private CanvasView canvasView;
    private ImageView colorIcon;

    @Inject
    DrawPresenter presenter;

    @ColorInt
    private int currentColor;

    private FragmentManager fragmentManager;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        canvasView = view.findViewById(R.id.canvas);
        colorIcon = view.findViewById(R.id.color);
        view.findViewById(R.id.color_pick_btn).setOnClickListener(v -> onColorPickButtonClick());
        view.findViewById(R.id.text_btn).setOnClickListener(v -> onTextButtonClick());
        view.findViewById(R.id.brush_btn).setOnClickListener(v -> onBrushButtonClick());

        fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dataFragment = (DataFragment) fragmentManager.findFragmentByTag(DataFragment.TAG);

            if (dataFragment == null) {
                dataFragment = new DataFragment();
                fragmentManager.beginTransaction()
                        .add(dataFragment, DataFragment.TAG)
                        .commit();
            } else {
                canvasView.setBitmap(dataFragment.getData());
            }
        }
        setHasOptionsMenu(true);
        presenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        presenter.unbindView(this);
        dataFragment.setData(canvasView.getBitmap());
        canvasView = null;
        colorIcon = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_btn:
                saveToFile();
                break;
            case R.id.upload_btn:
                loadImageFromGallery();
                break;
            case R.id.clear_btn:
                canvasView.clear();
                break;
            case R.id.share_btn:
                share();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onColorPickButtonClick() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        ColorPickerDialogBuilder
                .with(context)
                .setTitle(R.string.color_pick)
                .initialColor(currentColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(10)
                .setPositiveButton(R.string.ok, (dialog, selectedColor, allColors) -> colorPicks.onNext(selectedColor))
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .build()
                .show();
    }

    private void onTextButtonClick() {
        TextDialogFragment textDialogFragment = new TextDialogFragment();
        textDialogFragment.setTargetFragment(this, TEXT_PICKER_REQUEST_CODE);
        textDialogFragment.show(fragmentManager, TextDialogFragment.TAG);
    }

    private void onBrushButtonClick() {
        canvasView.setBrush();
        BrushPickerDialogFragment brushPickerDialogFragment = new BrushPickerDialogFragment();
        brushPickerDialogFragment.setTargetFragment(this, BRUSH_PICKER_REQUEST_CODE);
        brushPickerDialogFragment.show(fragmentManager, BrushPickerDialogFragment.TAG);
    }

    private void saveToFile() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            try {
                ImageUtils.saveImageToFile(getContext(), canvasView.getBitmap());
            } catch (IOException e) {
                Timber.e(e);
            }
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    SAVE_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
    }

    public void loadImageFromGallery() {
        Intent takeGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(
                Intent.createChooser(
                        takeGalleryIntent,
                        getString(R.string.choose_photo)),
                GALLERY_PICTURE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (requestCode == GALLERY_PICTURE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            canvasView.setBitmap(ImageUtils.loadBitmapFromUri(context, imageUri));
        }
    }

    @Override
    public void onEditText(String text) {
        canvasView.setText(text);
    }


    @Override
    public void onBrushPick(float brushWidth) {
        canvasView.setStrokeWidth(brushWidth);
    }


    @Override
    public Observable<Integer> colorPicks() {
        return colorPicks;
    }

    @Override
    public void setStrokeWidth(float savedStrokeWidth) {
        canvasView.setStrokeWidth(savedStrokeWidth);
    }

    @Override
    public void setColor(@ColorInt int color) {
        currentColor = color;
        canvasView.setColor(color);
    }

    @Override
    public void updateMenuColor(@ColorInt int color) {
        DrawableCompat.setTint(colorIcon.getDrawable(), color);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == SAVE_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
                saveToFile();
            } else if (requestCode == SHARE_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
                share();
            }
        } else {
            Toast.makeText(getContext(), R.string.need_permissions, Toast.LENGTH_SHORT).show();
        }
    }

    private void share() {
        Context context = getContext();
        if (context != null && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            String bitmapPath = MediaStore.Images.Media.insertImage(
                    getContext().getContentResolver(),
                    canvasView.getBitmap(),
                    String.valueOf(System.currentTimeMillis()),
                    null);
            Uri bitmapUri = Uri.parse(bitmapPath);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
            shareIntent.setType("image/*");
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    SHARE_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
    }
}
