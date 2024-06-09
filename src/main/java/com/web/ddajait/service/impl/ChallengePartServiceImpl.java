package com.web.ddajait.service.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.web.ddajait.model.dao.ChallengeInfoDao;
import com.web.ddajait.model.dao.ChallengePartDao;
import com.web.ddajait.model.dao.PartQuestionDao;
import com.web.ddajait.model.dto.ChallengePartDto;
import com.web.ddajait.model.dto.ChallengePart.Challenge;
import com.web.ddajait.model.dto.ChallengePart.Chapter;
import com.web.ddajait.model.dto.ChallengePart.Day;
import com.web.ddajait.model.dto.ChallengePart.Step;
import com.web.ddajait.model.dto.ChallengePart.TestQuestion;
import com.web.ddajait.model.entity.ChallengeInfoEntity;
import com.web.ddajait.model.entity.ChallengePartEntity;
import com.web.ddajait.model.entity.PartQuestionEntity;
import com.web.ddajait.service.ChallengePartService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengePartServiceImpl implements ChallengePartService {

    private final ChallengePartDao challengePartDao;
    private final PartQuestionDao partQuestionDao;
    private final ChallengeInfoDao challengeInfoDao;

    @Override
    public List<ChallengePartDto> getAllchallengePartInfo() {
        log.info("[challengePartServiceImpl][getAllchallengePartInfo] Starts");
        return challengePartDao.getAllChallenge().stream()
                .map(ChallengePartDto::from)
                .collect(Collectors.toList());

    }

    @Override
    public ChallengePartDto getchallengePartInfo(Long challengePartId) {
        if (challengePartDao.findChallengeById(challengePartId).isPresent()) {
            ChallengePartEntity entity = challengePartDao.findChallengeById(challengePartId).get();
            return ChallengePartDto.from(entity);

        } else {
            throw new EntityNotFoundException("Not found ChallengePartEntity");

        }

    }

    @Override
    public Challenge getChallengersDetailData(Long challengeId) {

        Challenge challenge = new Challenge();

        if (challengeInfoDao.findById(challengeId).isPresent()) {
            ChallengeInfoEntity challengeInfoentity = challengeInfoDao.findById(challengeId).get();
            String name = challengeInfoentity.getChallengeName();

            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");

            Timestamp starTimestamp = challengeInfoentity.getStartDay();
            Timestamp endTimestamp = challengeInfoentity.getEndDay();

            long durationInMillis = endTimestamp.getTime() - starTimestamp.getTime();

            // 결과를 일 단위로 변환합니다.
            long period = durationInMillis / (1000 * 60 * 60 * 24);

            String startDay = outputFormat.format(starTimestamp);
            String endDay = outputFormat.format(endTimestamp);
            int myProgress = 20;
            int totalProgress = 40;
            int totalUser = 10;

            challenge.setName(name);
            challenge.setStart(startDay);
            challenge.setEnd(endDay);
            challenge.setMy_progress(myProgress);
            challenge.setTotal_progress(totalProgress);
            challenge.setTotal_user(totalUser);

            List<ChallengePartEntity> partEntityList = challengePartDao.findChallengePartByChallengeId(challengeId);

            List<Step> steps = partEntityList.stream().map(source -> {

                Step step = new Step();
                log.info("[challengePartServiceImpl][getChallengersDetailData] source " + source.getSectionName());

                int step_num = source.getPartNum();
                boolean complete = true;
                String part_name = source.getPartName();
                String chapter_name = source.getChapterName();
                String section_name = source.getSectionName();

                step.setStep(step_num);
                step.setComplete(complete);
                step.setChapterName(chapter_name);
                step.setPartName(part_name);
                step.setSectionName(section_name);

                Long certifocatePartId = source.getCertificatePartInfo().getCertificatePartId();
                List<PartQuestionEntity> partQuestionEntities = partQuestionDao
                        .findByCetificatePartId(certifocatePartId);

                AtomicInteger testId = new AtomicInteger(0);
                // TestQuestion 생성
                List<TestQuestion> testQuestions = partQuestionEntities.stream().map(testData -> {
                    TestQuestion testQuestion = new TestQuestion();

                    int id = testId.incrementAndGet();

                    if (testData.getChoices().size() == 4) {
                        testQuestion.setId(id);
                        testQuestion.setQuestion(testData.getQuestion());
                        testQuestion.setItem1(testData.getChoices().get(0));
                        testQuestion.setItem2(testData.getChoices().get(1));
                        testQuestion.setItem3(testData.getChoices().get(2));
                        testQuestion.setItem4(testData.getChoices().get(3));
                        testQuestion.setAnswer(testData.getAnswer());
                        return Optional.of(testQuestion);

                    } else {
                        return Optional.<TestQuestion>empty();

                    }

                })
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                step.setTest(testQuestions);

                List<Day> days = partEntityList.stream().map(partData -> {

                    Day day = new Day();

                    day.setDay(partData.getDay());

                    Chapter chapter = new Chapter();
                    chapter.setName(partData.getPartName());

                    List<String> sections = partEntityList.stream()
                            .filter(p -> p.getDay() == partData.getDay())
                            .map(ChallengePartEntity::getSectionName)
                            .collect(Collectors.toList());

                    chapter.setSection(sections);

                    day.setChapter(chapter);
                    day.setComplete(true);
                    day.setMemo("memo 입니다");

                    return day;

                }).collect(Collectors.toList());

                step.setDays(days);

                return step;

            }).collect(Collectors.toList());

            challenge.setSteps(steps);

            return challenge;
        } else {
            throw new EntityNotFoundException("Not found ChallengePartEntityList");

        }
    }

}