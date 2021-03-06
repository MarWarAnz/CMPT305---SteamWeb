package cmpt305.lab3.gui.controllers;

import cmpt305.lab3.exceptions.APIEmptyResponse;
import cmpt305.lab3.gui.views.MainScreenView;
import cmpt305.lab3.structure.Genre;
import cmpt305.lab3.structure.User;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.MapChangeListener;
import javafx.collections.SetChangeListener;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

public class MainScreenController{
	private final MainScreenView VIEW;
	private final SettingsController SETTINGS;
	private final GetUserController GET_USER_CONTROLLER = new GetUserController();
	private final GenreListModel GENRES = new GenreListModel();
	private final DefaultListModel USERS = new DefaultListModel();
	private final GenreDataSetListener GENRES_LISTENER = new GenreDataSetListener();
	private final UserDataSetListener USERS_LISTENER = new UserDataSetListener();

	private CompareGraph graph;

	/*
	 NOTE: invokeLater is used to ensure thread safety when adding or removing
	 data from the GenreListModel.
	 */
	private class GenreListModel extends AbstractListModel{
		private final List<String> GENRE_LIST;

		@Override
		public int getSize(){
			return GENRE_LIST.size();
		}

		@Override
		public String getElementAt(int i){
			return GENRE_LIST.get(i);
		}

		public void addElement(final String str){
			SwingUtilities.invokeLater(() -> {
				GENRE_LIST.add(str);
				int index = GENRE_LIST.indexOf(str);
				fireIntervalAdded(GenreListModel.this, index, index);
				sort();
			});
		}

		public void removeElement(final String str){
			SwingUtilities.invokeLater(() -> {
				int index = GENRE_LIST.indexOf(str);
				GENRE_LIST.remove(str);
				fireIntervalRemoved(GenreListModel.this, index, index);
				sort();
			});

		}

		public void sort(){
			Collections.sort(GENRE_LIST);
			fireContentsChanged(this, 0, GENRE_LIST.size() - 1);
		}

		public GenreListModel(){
			super();
			GENRE_LIST = new ArrayList();
		}

	}

	private class GenreDataSetListener implements SetChangeListener{
		@Override
		public void onChanged(Change change){
			if(change.wasAdded()){
				GENRES.addElement(change.getElementAdded().toString());
			}
			if(change.wasRemoved()){
				GENRES.removeElement(change.getElementRemoved().toString());
			}
		}
	}

	private class UserDataSetListener implements MapChangeListener{
		@Override
		public void onChanged(Change change){
			if(change.wasAdded()){
				User user = (User) change.getValueAdded();
				USERS.addElement(user.getVanity());
			}
			if(change.wasRemoved()){
				User user = (User) change.getValueRemoved();
				USERS.removeElement(user.getVanity());
			}
		}
	}

	private void showAddUser(){
		GET_USER_CONTROLLER.toggle();
	}

	public MainScreenController(){
		User.addListener(USERS_LISTENER);
		Genre.addListener(GENRES_LISTENER);

		for(Genre g : Genre.getKnown()){
			GENRES.addElement(g.toString());
		}

		VIEW = new MainScreenView();
		VIEW.pack();
		VIEW.setGenreModel(GENRES);
		VIEW.setUserModel(USERS);
		VIEW.setVisible(true);

		SETTINGS = new SettingsController();

		VIEW.addSettingButtonListener(ae -> SETTINGS.toggle());
		VIEW.addClearButtonListener(ae -> clearUserCompare());
		VIEW.addUserButtonListener(ae -> showAddUser());
		VIEW.addCompareButtonListener(ae -> createGraph());
	}

	private void createGraph(){
		if(USERS.size() < 2){
			System.err.println("There must be at least two users to compare.");
			return;
		}
		try{
			User main = User.getUser(USERS.get(0).toString());
			Set<User> others = new HashSet();
			for(int i = 1; i < USERS.size(); i++){
				others.add(User.getUser(USERS.get(i).toString()));
			}
			graph = new CompareGraph(main, others);
			graph.pack();
			graph.setVisible(true);
		}catch(APIEmptyResponse ex){
			System.err.println("Invalid vanity");
		}
	}

	public void addUserCompare(final String username){
		SwingUtilities.invokeLater(() -> USERS.addElement(username));
	}

	public void removeUserCompare(final String username){
		SwingUtilities.invokeLater(() -> USERS.removeElement(username));
	}

	private void clearUserCompare(){
		SwingUtilities.invokeLater(() -> USERS.clear());
	}
}
