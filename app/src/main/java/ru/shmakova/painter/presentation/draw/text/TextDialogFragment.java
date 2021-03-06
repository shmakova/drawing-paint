package ru.shmakova.painter.presentation.draw.text;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import ru.shmakova.painter.R;
import ru.shmakova.painter.presentation.base.BaseDialogFragment;
import rx.Observable;
import rx.subjects.PublishSubject;

public class TextDialogFragment extends BaseDialogFragment implements TextDialogView {
    public static final String TAG = "fragment_text";

    @NonNull
    private final PublishSubject<String> submitClicks = PublishSubject.create();

    @Inject
    TextDialogPresenter presenter;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_text, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText editText = view.findViewById(R.id.edit_text);
        Button editTextButton = view.findViewById(R.id.edit_text_btn);
        editTextButton.setOnClickListener(v -> submitClicks.onNext(editText.getText().toString()));
        presenter.bindView(this);
    }

    @Override
    public void onDestroyView() {
        presenter.unbindView(this);
        super.onDestroyView();
    }

    @Override
    @NonNull
    public Observable<String> submitClicks() {
        return submitClicks;
    }

    @Override
    public void sendBackResult(@NonNull String text) {
        EditTextDialogListener listener = (EditTextDialogListener) getTargetFragment();
        if (listener != null) {
            listener.onEditText(text);
        }
    }

    @Override
    public void dismissDialog() {
        dismiss();
    }

    public interface EditTextDialogListener {
        void onEditText(String text);
    }
}
