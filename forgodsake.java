package com.oyster.connect.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.oyster.connect.Adapter.ModuleAdapter;
import com.oyster.connect.Adapter.ResponseAdapter;
import com.oyster.connect.Model.AnswerModel;
import com.oyster.connect.Model.CommonModel;
import com.oyster.connect.Model.OptionModel;
import com.oyster.connect.Model.PTQuestionModel;
import com.oyster.connect.Model.PreselectionSlideModel;
import com.oyster.connect.Model.QuestionBody;
import com.oyster.connect.Model.QuestionCommonModel;
import com.oyster.connect.Model.QuestionModel;
import com.oyster.connect.Model.ReviewResponseModel;
import com.oyster.connect.Model.SlideCommonModel;
import com.oyster.connect.Model.SubmitAnswerModel;
import com.oyster.connect.Model.SubmitReviewModel;
import com.oyster.connect.Model.UserIdBody;
import com.oyster.connect.R;
import com.oyster.connect.Retrofit.ServicesConnection;
import com.oyster.connect.Retrofit.ServicesInterface;
import com.oyster.connect.SharedPrefrence.SPreferenceKey;
import com.oyster.connect.SharedPrefrence.SharedPreferenceWriter;
import com.oyster.connect.Utility.CommonUtilities;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PreSelectionTask extends AppCompatActivity {

    @BindView(R.id.tv_questions)
    TextView tv_questions;
    @BindView(R.id.tv_next)
    TextView tv_next;
    @BindView(R.id.tv_back)
    TextView tv_back;
    @BindView(R.id.rv_options)
    ListView rv_options;
    @BindView(R.id.iv_icon)
    ImageView iv_icon;
    @BindView(R.id.btn_confirm)
    Button btn_confirm;
    @BindView(R.id.btn_fail_retake)
    Button btn_fail_retake;
    @BindView(R.id.tv_message)
    TextView tv_message;
    @BindView(R.id.tv_response)
    TextView tv_response;
    @BindView(R.id.iv_pre_slide)
    ImageView iv_pre_slide;
    @BindView(R.id.cl_module_count)
    RecyclerView cl_module_count;
    @BindView(R.id.module_desc)
    TextView module_desc;

    OptionModel[] answers = new OptionModel[100];
    ReviewResponseModel[] reviewansers = new ReviewResponseModel[100];

    private int currentQuestion = 0;
    private int currentSlide = 0;
    private int currentModule = 0;
    private boolean defaultsectionstarted = false;
    private boolean reviewSectionStarted = false;
    private boolean slideSectionStarted = false;
    private int nextIdentifier = 1;

    private ArrayList<QuestionModel> questions = new ArrayList<>();
    private ArrayList<OptionModel> options;
    private ArrayList<PreselectionSlideModel> slides;
    private ArrayList<PTQuestionModel> reviewQuestions = new ArrayList<>();
    private int TotalModules = 4;
    private int CurrentModule = 1;
    private int TotalReviewQuestions = 0;
    private ModuleAdapter moduleAdapter = new ModuleAdapter(currentModule,TotalModules,this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test2);

        ButterKnife.bind(this);
        tv_next.setClickable(false);
        tv_back.setClickable(false);
        DefaultQuestionApi();

        rv_options.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (options != null && options.size() > 0) {
                    answers[currentQuestion] = options.get(position);
                }
                Toast.makeText(PreSelectionTask.this, answers[currentQuestion].getOption(), Toast.LENGTH_SHORT).show();
                tv_next.setClickable(true);
                tv_next.setTextColor(Color.parseColor("#EE8036"));
            }
        });
    }

    private void DefaultQuestionApi() {
        if (CommonUtilities.isNetworkAvailable(this)) {
            try {
                ServicesInterface servicesInterface = ServicesConnection.getInstance().createService();
                String token = SharedPreferenceWriter.getInstance(this).getString(SPreferenceKey.kUserToken);
                Call<QuestionCommonModel> responesbean = servicesInterface.getQuestions(token, new QuestionBody("5efd7ed45f3e970b47ad94e9", "PartTime"));

                ServicesConnection.getInstance().enqueueWithoutRetry(responesbean, this, true, new Callback<QuestionCommonModel>() {
                    @Override
                    public void onResponse(Call<QuestionCommonModel> call, Response<QuestionCommonModel> response) {
                        if (response.isSuccessful()) {
                            if (response.body().getStatus().equalsIgnoreCase("true")) {
                                questions = response.body().getData();
                                if (questions != null) {
                                    defaultsectionstarted = true;
                                    module_desc.setText("Confirmation of Primary Requirements");
                                    setQuestionScreen();
//                                    setRecyclerViewForModules();
                                }
                            } else {
                                if (response.body().getMessage().equalsIgnoreCase("All Attemps Taken")) {
                                    tv_questions.setVisibility(View.VISIBLE);
                                    tv_questions.setText(response.body().getMessage());
                                } else {
                                    defaultsectionstarted = false;
                                    slideSectionStarted = true;
                                    currentModule++;
//                                    setRecyclerViewForModules();
                                    displayPreSelectionTask();
                                }
                            }
                        } else {
                            int responseError = response.code();
                            if (responseError == 401) {
                                Intent intent = new Intent(PreSelectionTask.this, LoginActivity.class);
                                startActivity(intent);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<QuestionCommonModel> call, Throwable t) {

                    }
                });
            } catch (Exception e) {
                Log.e("InternDetail exception", e.getMessage());
            }
        } else {
            CommonUtilities.snackBar(this, getString(R.string.check_connection));
        }
    }

    private void displayPreSelectionTask() {
        slideSectionStarted = true;
        PrepareScreenForSlides();
        preselectionApiHit();
    }

    private void preselectionApiHit() {
        if (CommonUtilities.isNetworkAvailable(this)) {
            try {
                ServicesInterface servicesInterface = ServicesConnection.getInstance().createService();
                String token = SharedPreferenceWriter.getInstance(this).getString(SPreferenceKey.kUserToken);
                Call<SlideCommonModel> responesbean = servicesInterface.getSlides(token, new UserIdBody("5efd7ed45f3e970b47ad94e9"));

                ServicesConnection.getInstance().enqueueWithoutRetry(responesbean, this, true, new Callback<SlideCommonModel>() {
                    @Override
                    public void onResponse(Call<SlideCommonModel> call, Response<SlideCommonModel> response) {
                        if (response.isSuccessful()) {
                            if (response.body().getStatus().equalsIgnoreCase("true")) {
                                slides = response.body().getData();
                                if (slides != null) {
                                    currentSlide = 0;
                                    setSlidesScreen();
                                    TotalModules = countModules();
                                    moduleAdapter.notifyDataSetChanged();
                                }
                            } else {
                            }
                        } else {
                            int responseError = response.code();
                            if (responseError == 401) {
                                Intent intent = new Intent(PreSelectionTask.this, LoginActivity.class);
                                startActivity(intent);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<SlideCommonModel> call, Throwable t) {

                    }
                });
            } catch (Exception e) {
                Log.e("InternDetail exception", e.getMessage());
            }
        } else {
            CommonUtilities.snackBar(this, getString(R.string.check_connection));
        }
    }

    private void setSlidesScreen() {
        if (slides.get(currentSlide).getDataType() == 0) {
            reviewSectionStarted = false;
            slideSectionStarted = true;
            defaultsectionstarted = false;
            module_desc.setText("Project Workflow & Guidelines");
            iv_pre_slide.setVisibility(View.VISIBLE);
            String slide = slides.get(currentSlide).getSlideUrl();
            if (slide != null && !slide.equalsIgnoreCase("")) {
                Glide.with(this).load(slide).into(iv_pre_slide);
            } else {
                Glide.with(this)
                        .load(R.drawable.logo_oyster_small)
                        .into(iv_pre_slide);
            }
        } else {
            reviewSectionStarted = true;
            slideSectionStarted = false;
            defaultsectionstarted = false;
            module_desc.setText("Review & Assessment");
            currentQuestion = 0;
            reviewQuestions = slides.get(currentSlide).getReviewSection();
            TotalReviewQuestions = slides.get(currentSlide).getReviewSection().get(0).getTotalQuestionCount();
            setPreSlectionQuestionScreen(reviewQuestions);
        }
    }

    private void setPreSlectionQuestionScreen(ArrayList<PTQuestionModel> reviewQuestions) {
        PrepareScreenForQuestionWindow();
        tv_next.setClickable(false);
        if (currentQuestion == reviewQuestions.size()) {
            tv_next.setText("Submit");
        } else {
            tv_next.setText("Next");
        }
        tv_questions.setText(reviewQuestions.get(currentQuestion).getQuestionText());
        options = reviewQuestions.get(currentQuestion).getOptions();
        setRecylerView(options);
    }

    private void setQuestionScreen() {
        PrepareScreenForQuestionWindow();
        tv_next.setClickable(false);
        if (currentQuestion == questions.size() - 1) {
            tv_next.setText("Submit");
        } else {
            tv_next.setText("Next");
        }
        tv_questions.setText(questions.get(currentQuestion).getQuestion());
        options = questions.get(currentQuestion).getOptions();
        setRecylerView(options);
    }

    private void submitResponse() {
        if (!questions.isEmpty()) {
            ArrayList<AnswerModel> response = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                String questionId = questions.get(i).get_id();
                int marks = 0;
                String answer = "";
                if (answers[i] != null) {
                    marks = answers[i].getMarks();
                    answer = answers[i].getOption();
                }
                response.add(new AnswerModel(questionId, answer, marks));
            }

            if (response != null) {
                submitApiHit(response);
            }
        }
    }

    private void submitApiHit(ArrayList<AnswerModel> res) {
        if (CommonUtilities.isNetworkAvailable(this)) {
            try {
                ServicesInterface servicesInterface = ServicesConnection.getInstance().createService();
                String token = SharedPreferenceWriter.getInstance(this).getString(SPreferenceKey.kUserToken);
                Call<CommonModel> responesbean = servicesInterface.submitResponse(token, new SubmitAnswerModel("5e0ed4499bfdb2184832f3a4", "1", res));
                ServicesConnection.getInstance().enqueueWithoutRetry(responesbean, this, true, new Callback<CommonModel>() {
                    @Override
                    public void onResponse(Call<CommonModel> call, Response<CommonModel> response) {
                        if (response.isSuccessful()) {
                            if (response.body().getStatus().equalsIgnoreCase("true")) {
                                if (response.body().getMessage().equalsIgnoreCase("Section Passed Successfully")) {
                                    tv_questions.setText(R.string.pre_slection_selected);
                                    tv_questions.setGravity(Gravity.CENTER);
                                    tv_next.setVisibility(View.GONE);
                                    iv_icon.setVisibility(View.VISIBLE);
                                    iv_icon.setImageResource(R.drawable.green_tick);
                                    btn_confirm.setVisibility(View.VISIBLE);
                                    setresponseScreen();
                                } else if (response.body().getMessage().equalsIgnoreCase("Section Failed")) {
                                    tv_questions.setText(R.string.pre_slection_rejected_title);
                                    tv_questions.setGravity(Gravity.CENTER);
                                    tv_message.setVisibility(View.VISIBLE);
                                    tv_message.setText(R.string.pre_slection_rejected);
                                    tv_next.setVisibility(View.GONE);
                                    iv_icon.setVisibility(View.VISIBLE);
                                    iv_icon.setImageResource(R.drawable.rejection_red);
                                    btn_fail_retake.setVisibility(View.VISIBLE);
                                    setresponseScreen();
                                }
                            } else {
                                if (response.body().getMessage().equalsIgnoreCase("All Attemps Taken"))
                                    tv_questions.setText(response.body().getMessage());
                                else {
                                    tv_questions.setText(response.body().getMessage());
                                }
                            }
                        } else {
                            int responseError = response.code();
                            if (responseError == 401) {
                                Intent intent = new Intent(PreSelectionTask.this, LoginActivity.class);
                                startActivity(intent);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommonModel> call, Throwable t) {

                    }
                });
            } catch (Exception e) {
                Log.e("InternDetail exception", e.getMessage());
            }
        } else {
            CommonUtilities.snackBar(this, getString(R.string.check_connection));
        }
    }

    private void submitReviewResponse() {
        int totalmarks = 0;
        int achieveMarks = 0;
        ArrayList<ReviewResponseModel> response = new ArrayList<>();
        if (!reviewQuestions.isEmpty()) {
            for (int i = 0; i < reviewQuestions.size(); i++) {
                response.add(new ReviewResponseModel(0, reviewQuestions.get(i).getCurrentQuestionCount(), answers[i].getMarks(), answers[i].getOption()));
                achieveMarks += answers[i].getMarks();
                totalmarks += reviewQuestions.get(i).getTotalQuestionCount();
            }
        }
        if (response != null) {
            submitReviewResponseApiHit(response, totalmarks, achieveMarks);
        }
    }

    private void submitReviewResponseApiHit(ArrayList<ReviewResponseModel> response, int totalmarks, int achievemarks) {
        if (CommonUtilities.isNetworkAvailable(this)) {
            try {
                ServicesInterface servicesInterface = ServicesConnection.getInstance().createService();
                String token = SharedPreferenceWriter.getInstance(this).getString(SPreferenceKey.kUserToken);
                Call<CommonModel> responesbean = servicesInterface.getReview(token, new SubmitReviewModel(currentModule, totalmarks, achievemarks, "5e0ed4499bfdb2184832f3a4", response));

                ServicesConnection.getInstance().enqueueWithoutRetry(responesbean, this, true, new Callback<CommonModel>() {
                    @Override
                    public void onResponse(Call<CommonModel> call, Response<CommonModel> response) {
                        if (response.isSuccessful()) {
                            if (response.body().getStatus().equalsIgnoreCase("true")) {
                                if (response.body().getMessage().equalsIgnoreCase("Response recorded Successfully")) {
                                    displayReviewResponseScreen(true);
                                }
                            }else if (response.body().getStatus().equalsIgnoreCase("1")){
                                if (response.body().getMessage().equalsIgnoreCase("Review section not Qualified")){
                                    displayReviewResponseScreen(false);
                                }
                            }
                        } else {
                            int responseError = response.code();
                            if (responseError == 401) {
                                Intent intent = new Intent(PreSelectionTask.this, LoginActivity.class);
                                startActivity(intent);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommonModel> call, Throwable t) {

                    }
                });
            } catch (Exception e) {
                Log.e("InternDetail exception", e.getMessage());
            }
        } else {
            CommonUtilities.snackBar(this, getString(R.string.check_connection));
        }
    }

    private void displayReviewResponseScreen(boolean sectionClearedOrNot) {
        String response = "";
        for (int i = 0; i < reviewQuestions.size(); i++) {
            response += reviewQuestions.get(i).getQuestionText() + "\t" + answers[i].getOption() + "\n";
        }

        PrepareScreenForResponseSection(sectionClearedOrNot, response);

    }

    private void setresponseScreen() {
        rv_options.setVisibility(View.GONE);
        String response = "";
        if (questions != null && answers != null) {
            for (int i = 0; i < questions.size(); i++) {
                response += questions.get(i).getQuestion() + "\t" + ":" + "\t" + answers[i].getOption() + "\n";
            }
            tv_response.setText(response);
            tv_response.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.tv_next)
    public void click() {
        if (defaultsectionstarted || reviewSectionStarted) {
            currentQuestion++;
            tv_next.setClickable(false);
            tv_next.setTextColor(Color.parseColor("#8a8a8a"));
            if (defaultsectionstarted) {
                if (currentQuestion == questions.size() - 1) {
                    submitResponse();
                } else {
                    setQuestionScreen();
                }
            } else if (reviewSectionStarted) {
                if (currentQuestion == reviewQuestions.size()) {
                    submitReviewResponse();
                } else {
                    setPreSlectionQuestionScreen(reviewQuestions);
                }
            }
        } else if (slideSectionStarted) {
            currentSlide++;
            setSlidesScreen();
        }
    }

    @OnClick(R.id.tv_back)
    public void onBackClick() {
        if (currentSlide > 0 && slides != null) {
            setSlidesScreen();
        }
    }

    @OnClick(R.id.btn_confirm)
    public void onclick() {
        if (defaultsectionstarted) {
            defaultsectionstarted = false;
            questions.clear();
            options.clear();
            currentQuestion = 0;
            answers = new OptionModel[100];
            PrepareScreenForSlides();
            displayPreSelectionTask();
            moduleAdapter.notifyDataSetChanged();
        } else if (reviewSectionStarted) {
            reviewSectionStarted = false;
            currentQuestion = 0;
            TotalReviewQuestions = 0;
            options.clear();
            reviewQuestions.clear();
            currentSlide++;
            answers = new OptionModel[100];
            currentModule++;
            moduleAdapter.notifyDataSetChanged();
            PrepareScreenForSlides();
            setSlidesScreen();
        }
    }

    @OnClick(R.id.btn_fail_retake)
    public void onClick() {
        finish();
        startActivity(getIntent());
    }

    private void setRecylerView(ArrayList<OptionModel> list) {
        ArrayList<String> fill = new ArrayList<>();
        for (OptionModel op : list) {
            fill.add(op.getOption());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, fill);
        rv_options.setAdapter(adapter);
    }

    private void  setRecyclerViewForModules(){
        cl_module_count.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,true));
        cl_module_count.setAdapter(moduleAdapter);
    }

    private void PrepareScreenForQuestionWindow() {
        tv_questions.setVisibility(View.VISIBLE);
        tv_next.setVisibility(View.VISIBLE);
        rv_options.setVisibility(View.VISIBLE);
        iv_icon.setVisibility(View.GONE);
        tv_message.setVisibility(View.GONE);
        btn_fail_retake.setVisibility(View.GONE);
        btn_confirm.setVisibility(View.GONE);
        tv_response.setVisibility(View.GONE);
        iv_pre_slide.setVisibility(View.GONE);
    }

    private void PrepareScreenForSlides() {
        tv_next.setClickable(true);
        iv_pre_slide.setVisibility(View.VISIBLE);
        tv_next.setVisibility(View.VISIBLE);
        tv_back.setVisibility(View.VISIBLE);
        tv_questions.setVisibility(View.GONE);
        tv_response.setVisibility(View.GONE);
        rv_options.setVisibility(View.GONE);
        iv_icon.setVisibility(View.GONE);
        tv_message.setVisibility(View.VISIBLE);
        tv_message.setText("Reading Time : 20 mins");
        btn_fail_retake.setVisibility(View.GONE);
        btn_confirm.setVisibility(View.GONE);
    }

    private void PrepareScreenForResponseSection(boolean passed, String response) {
        iv_pre_slide.setVisibility(View.GONE);
        tv_next.setVisibility(View.GONE);
        tv_back.setVisibility(View.GONE);
        tv_questions.setVisibility(View.VISIBLE);
        tv_response.setVisibility(View.VISIBLE);
        tv_response.setText(response);
        rv_options.setVisibility(View.GONE);
        iv_icon.setVisibility(View.VISIBLE);
        if (passed) {
            btn_confirm.setVisibility(View.VISIBLE);
            iv_icon.setImageResource(R.drawable.green_tick);
            tv_message.setVisibility(View.VISIBLE);
            tv_questions.setGravity(Gravity.CENTER);
            tv_questions.setText("Section Cleared");
            tv_message.setText("Move to next Section");
        } else {
            tv_questions.setText("Section Failed");
            iv_icon.setImageResource(R.drawable.rejection_red);
            btn_fail_retake.setVisibility(View.VISIBLE);
            tv_message.setVisibility(View.GONE);
        }
    }

    private int countModules() {
        int countModules = 0;
        if (slides != null) {
            for (int i = 0; i < slides.size(); i++) {
                if (slides.get(i).getDataType() == 1) {
                    countModules++;
                }
            }
        }
        return countModules + 1;
    }
}
