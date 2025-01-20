package org.example.taskservice.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.taskservice.dto.TaskRequestDto;
import org.example.taskservice.dto.TaskResponseDto;
import org.example.taskservice.entity.Task;
import org.example.taskservice.entity.TaskPriority;
import org.example.taskservice.entity.TaskStatus;
import org.example.taskservice.entity.User;
import org.example.taskservice.repository.CommentRepository;
import org.example.taskservice.repository.TaskRepository;
import org.example.taskservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TestTaskService {


    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private MapperService mapperService;

    @InjectMocks
    private TaskService taskService;

    @Test
    public void createTaskTest() throws IOException {
        TaskRequestDto taskRequestDto = TaskRequestDto.builder()
                .name("test")
                .description("test")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.IN_WAITING)
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("test")
                .role("ROLE_ADMIN")
                .build();

        Task task = Task.builder()
                .id(1L)
                .name("test")
                .description("test")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.IN_WAITING)
                .author(user)
                .build();

        when(userService.getClaimsFromToken(any(HttpServletRequest.class))).thenReturn(user);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(mapperService.mapToTask(eq(taskRequestDto), any(Task.class))).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Long taskId = taskService.createTask(taskRequestDto, mock(HttpServletRequest.class));

        assertNotNull(taskId);
        assertEquals(task.getId(), taskId);

        verify(mapperService).mapToTask(eq(taskRequestDto), any(Task.class));
        verify(taskRepository).save(task);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();

        assertEquals(task.getName(), savedTask.getName());
        assertEquals(task.getDescription(), savedTask.getDescription());
        assertEquals(task.getPriority(), savedTask.getPriority());
        assertEquals(task.getStatus(), savedTask.getStatus());
        assertEquals(user, savedTask.getAuthor());
    }

    @Test
    public void updateTaskTest() throws IOException {
        TaskRequestDto taskRequestDto = TaskRequestDto.builder()
                .name("test")
                .description("test")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.IN_WAITING)
                .build();

        Task task = Task.builder()
                .id(1L)
                .name("test")
                .description("test")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.IN_WAITING)
                .build();

        when(taskRepository.findById(any(Long.class))).thenReturn(Optional.of(task));
        when(mapperService.mapToTask(eq(taskRequestDto), any(Task.class))).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Long taskId = taskService.updateTask(1L, taskRequestDto);

        assertEquals(task.getId(), taskId);

        verify(mapperService).mapToTask(eq(taskRequestDto), any(Task.class));
        verify(taskRepository).save(task);
    }


    @Test
    public void testUpdateTaskExecutor() {

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("test")
                .role("ROLE_EXECUTOR")
                .build();

        Task task = Task.builder()
                .id(1L)
                .name("Test Task")
                .status(TaskStatus.IN_WAITING)
                .priority(TaskPriority.HIGH)
                .executors(new HashSet<>(Set.of(user)))
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userService.saveExecutor(eq(user.getId()), any(HttpServletRequest.class))).thenReturn(user);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Long updatedTaskId = taskService.updateTaskExecutor(1L, user.getId(), mock(HttpServletRequest.class));

        assertEquals(Long.valueOf(1), updatedTaskId);
        verify(taskRepository).save(any(Task.class));
    }


    @Test
    public void testDeleteTask() {
        Long taskId = 1L;
        doNothing().when(taskRepository).deleteById(taskId);

        taskService.deleteTask(taskId);

        verify(taskRepository).deleteById(taskId);
    }


    @Test
    public void testGetTasksByAuthor() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Task task = Task.builder()
                .id(1L)
                .name("Test Task")
                .description("Test Task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.IN_WAITING)
                .build();

        Page<Task> tasks = new PageImpl<>(List.of(task));

        when(taskRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(tasks);

        TaskResponseDto taskResponseDto = TaskResponseDto.builder()
                .name("Test Task")
                .status(TaskStatus.IN_WAITING)
                .priority(TaskPriority.HIGH)
                .description("Test Task")
                .build();
        when(mapperService.convertToTaskResponseDto(any(Task.class))).thenReturn(taskResponseDto);

        Page<TaskResponseDto> taskDto = taskService.getTasksByAuthor(1L, 0, 10, null, null);

        assertNotNull(taskDto);
        assertEquals(1, taskDto.getTotalElements()); // Проверяем количество элементов
        assertEquals("Test Task", taskDto.getContent().get(0).getName()); // Проверяем имя задачи
        assertEquals(TaskStatus.IN_WAITING, taskDto.getContent().get(0).getStatus()); // Проверяем статус задачи
        assertEquals(TaskPriority.HIGH, taskDto.getContent().get(0).getPriority()); // Проверяем приоритет задачи
    }
}
